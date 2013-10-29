package vu.exchange;

import static java.lang.String.format;

import org.apache.log4j.Logger;

import vu.exchange.LoginResult.LoginStatus;
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
		eventProcessor.process(new ResponseDTO(requestDTO, dispatchRequest(requestDTO.request)));
		log.debug("Request processed, response sent");
	}
	
	ApiResponse dispatchRequest(ApiRequest request) {
		if (request instanceof Order) {
			return order((Order)request);
		} else if (request instanceof Login) {
			return login((Login) request);
		} else {
			throw new IllegalArgumentException("request not supported: " + request);
		}

	}

	private LoginResult login(Login login) {
		return new LoginResult().withStatus(LoginStatus.OK).withSessionId("sessionId");
	}

	private OrderSubmitResult order(Order order) {
		return new OrderSubmitResult().withOrderId(order.id().toString()).withStatus(OrderStatus.ACCEPTED);
	}
}

