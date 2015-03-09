package org.jumpmind.symmetric.is.core.runtime.component;

import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

@ComponentDefinition(category = ComponentCategory.PROCESSOR, typeName = NoOpProcessor.TYPE, supports = {
        ComponentSupports.INPUT_MESSAGE, ComponentSupports.OUTPUT_MESSAGE })

public class NoOpProcessor extends AbstractComponent {

	public static final String TYPE="No Op";
		
	@Override
	public void handle(Message inputMessage, IMessageTarget messageTarget) {
		componentStatistics.incrementInboundMessages();
		messageTarget.put(inputMessage);
	}

}