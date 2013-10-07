package vu.exchange;

import org.apache.log4j.Logger;

import com.lmax.disruptor.EventHandler;

public class Publisher implements EventHandler<ValueEvent>{
	private final Logger log = Logger.getLogger(this.getClass());

	@Override
	public void onEvent(ValueEvent event, long sequence, boolean endOfBatch) throws Exception {
		log.info(String.format("Pushing event to client: %s", event.getValue()));
	}
}
