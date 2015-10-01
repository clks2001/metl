package org.jumpmind.metl.core.runtime.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.ShutdownMessage;
import org.jumpmind.metl.core.runtime.StartupMessage;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class JoinerTest extends AbstractComponentRuntimeTest<ArrayList<EntityData>> {

	@Test
	@Override
	public void testHandleStartupMessage() {
		setInputMessage(new StartupMessage());
		runHandle();
		assertHandle(1,0, getExpectedMonitorSingle(0, 0, 0, -1, 0));
	}

	@Test
	@Override
	public void testHandleUnitOfWorkLastMessage() {
		setupHandle();
		setUnitOfWorkLastMessage(true);
		
		getInputMessage().setPayload(new ArrayList<EntityData>());
		
		runHandle();
		assertHandle(1,0, getExpectedMonitorSingle(0, 0, 0, -1, 0));
	}

	@Test
	@Override
	public void testHandleNormal() {
		setupHandle();
		setUnitOfWorkLastMessage(true);
		
		getInputMessage().setPayload(PayloadTestHelper.createPayloadWithMultipleEntityData());
		
		List<String> attributesToJoinOn = new ArrayList<String>();
		attributesToJoinOn.add(MODEL_ATTR_ID_1);
		attributesToJoinOn.add(MODEL_ATTR_ID_2);
		((Joiner) spy).attributesToJoinOn = attributesToJoinOn;
		
		runHandle();
		assertHandle(1,1, getExpectedMonitorSingle(1, 0, 0, 0, 1));
		
		Map<Object, EntityData> joinedData = ((Joiner) spy).joinedData;
		
		assertEquals(1, joinedData.size());
		String expectedKey = MODEL_ATTR_ID_1 + "=" + MODEL_ATTR_NAME_1 + "&" + MODEL_ATTR_ID_2 + "=" + MODEL_ATTR_NAME_2;
		assertTrue(joinedData.containsKey(expectedKey));
		EntityData expectedEntity = joinedData.get(expectedKey);
		assertEquals(3, expectedEntity.size());
	}

	@Override
	public IComponentRuntime getComponentSpy() {
		return spy(new Joiner());
	}
	
	
}
