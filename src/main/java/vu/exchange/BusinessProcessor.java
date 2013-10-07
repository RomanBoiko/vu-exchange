package vu.exchange;

import org.apache.log4j.Logger;

import com.lmax.disruptor.EventHandler;

public class BusinessProcessor implements EventHandler<ValueEvent>{
	private final Logger log = Logger.getLogger(this.getClass());
	private final CallableProcessor eventProcessor;

	BusinessProcessor(CallableProcessor eventProcessor) {
		this.eventProcessor = eventProcessor;
	}

	public void onEvent(final ValueEvent event, final long sequence, final boolean endOfBatch) throws Exception {
		log.info(String.format("Processing business event: Sequence: %s; Thread: %s; ValueEvent: %s", sequence, Thread.currentThread().getName(), event.getValue()));
		eventProcessor.process(OrderStatus.ORDER_ACCEPTED);
		log.info("Order accepted, status sent");
	}
}

