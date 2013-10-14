package vu.exchange;

import static java.lang.String.format;

import org.apache.log4j.Logger;

import vu.exchange.RequestResponseRepo.RequestDTO;
import vu.exchange.RequestResponseRepo.ResponseDTO;

import com.lmax.disruptor.EventHandler;

public class BusinessProcessor implements EventHandler<ValueEvent>{
	private final Logger log = Logger.getLogger(this.getClass());
	private final ExchangeDisruptor eventProcessor;

	BusinessProcessor(ExchangeDisruptor eventProcessor) {
		this.eventProcessor = eventProcessor;
	}

	public void onEvent(final ValueEvent event, final long sequence, final boolean endOfBatch) throws Exception {
		RequestDTO requestDTO = (RequestDTO) event.getValue();
		log.debug(format("Processing business event: Sequence: %s; Thread: %s; ValueEvent: %s", sequence, Thread.currentThread().getName(), event.getValue()));
		eventProcessor.process(new ResponseDTO(requestDTO, OrderStatus.ORDER_ACCEPTED));
		log.debug("Request processed, response sent");
	}
}

