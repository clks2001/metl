/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.core.persist;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.model.Execution;
import org.jumpmind.metl.core.model.ExecutionStatus;
import org.jumpmind.metl.core.model.ExecutionStep;
import org.jumpmind.metl.core.model.ExecutionStepLog;
import org.jumpmind.persist.IPersistenceManager;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

abstract public class AbstractExecutionService extends AbstractService implements IExecutionService {
    
    ThreadPoolTaskScheduler purgeScheduler;
    
    Environment environment;
    
    public AbstractExecutionService(IPersistenceManager persistenceManager, String tablePrefix, Environment env) {
        super(persistenceManager, tablePrefix);
        this.environment = env;
        this.purgeScheduler = new ThreadPoolTaskScheduler();
        this.purgeScheduler.setThreadNamePrefix("execution-purge-job-");
        this.purgeScheduler.setPoolSize(1);
        this.purgeScheduler.initialize();
        this.purgeScheduler.setDaemon(true);
        this.purgeScheduler.scheduleWithFixedDelay(new PurgeExecutionHandler(), 60000*5);        
    }

    public Execution findExecution(String id) {
    	Execution e = new Execution();
    	e.setId(id);
        persistenceManager.refresh(e, null, null, tableName(e.getClass()));
        return e;
    }

    public List<ExecutionStep> findExecutionSteps(String executionId) {
    	Map<String, Object> args = new HashMap<String, Object>();
    	args.put("executionId", executionId);
    	List<ExecutionStep> steps = persistenceManager.find(ExecutionStep.class, args, null, null, tableName(ExecutionStep.class));
    	Collections.sort(steps, new Comparator<ExecutionStep>() {
    	    @Override
    	    public int compare(ExecutionStep o1, ExecutionStep o2) {
    	        int order = new Integer(o1.getApproximateOrder()).compareTo(new Integer(o2.getApproximateOrder()));
    	        if (order == 0) {
    	            order = new Integer(o1.getThreadNumber()).compareTo(new Integer(o2.getThreadNumber()));
    	        }
    	        return order;
    	    }
        });
    	return steps;
    }

    public List<ExecutionStepLog> findExecutionStepLog(String executionStepId) {
    	Map<String, Object> args = new HashMap<String, Object>();
    	args.put("executionStepId", executionStepId);
    	return persistenceManager.find(ExecutionStepLog.class, args, null, null, tableName(ExecutionStepLog.class));
    }
    
    abstract public void purgeExecutions(String status, int retentionTimeInMs);    
    
    class PurgeExecutionHandler implements Runnable {
        @Override
        public void run() {
            ExecutionStatus[] toPurge = new ExecutionStatus[] { ExecutionStatus.CANCELLED, ExecutionStatus.DONE, ExecutionStatus.ERROR, ExecutionStatus.ABANDONED };
            for (ExecutionStatus executionStatus : toPurge) {
                String retentionTimeInMs = environment.getProperty("execution.retention.time.ms", Long.toString(1000*60*60*24*7));
                retentionTimeInMs = environment.getProperty("execution.retention.time.ms." + executionStatus.name().toLowerCase(), retentionTimeInMs);
                purgeExecutions(executionStatus.name(), Integer.parseInt(retentionTimeInMs));
            }
        }
    }

}