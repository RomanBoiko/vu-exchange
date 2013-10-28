package vu.exchange;

import static java.lang.String.format;

import org.apache.log4j.Logger;

import vu.exchange.OrderSubmitResult.OrderStatus;
import vu.exchange.RequestResponseRepo.RequestDTO;
import vu.exchange.RequestResponseRepo.ResponseDTO;

import com.lmax.disruptor.EventHandler;

public class BusinessProcessor implements EventHandler<ValueEvent>{
	private final Logger log = Logger.getLogger(this.getClass());
	private final DisruptorWrapper eventProcessor;

	BusinessProcessor(DisruptorWrapper eventProcessor) {
		this.eventProcessor = eventProcessor;
	}

	public void onEvent(final ValueEvent event, final long sequence, final boolean endOfBatch) throws Exception {
		RequestDTO requestDTO = (RequestDTO) event.getValue();
		log.debug(format("Processing business event: Sequence: %s; ValueEvent: %s", sequence, event.getValue()));
		Order requestOrder = (Order)requestDTO.request;
		OrderSubmitResult result = new OrderSubmitResult().withOrderId(requestOrder.id.toString()).withStatus(OrderStatus.ACCEPTED);
		eventProcessor.process(new ResponseDTO(requestDTO, result));
		log.debug("Request processed, response sent");
	}
}

