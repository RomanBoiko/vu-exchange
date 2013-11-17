package vu.exchange;

import static java.lang.String.format;


import org.apache.log4j.Logger;

import vu.exchange.RequestResponseRepo.RequestDTO;
import vu.exchange.RequestResponseRepo.ResponseDTO;

import com.lmax.disruptor.EventHandler;

public class BusinessProcessor implements EventHandler<ValueEvent>{
	private final Logger log = Logger.getLogger(this.getClass());
	private final DisruptorBridge eventProcessor;

	private LoginProcessor loginProcessor = new LoginProcessor();
	private OrderProcessor orderProcessor = new OrderProcessor();
	
	BusinessProcessor(DisruptorBridge eventProcessor) {
		this.eventProcessor = eventProcessor;
	}

	public void onEvent(final ValueEvent event, final long sequence, final boolean endOfBatch) throws Exception {
		RequestDTO requestDTO = (RequestDTO) event.getValue();
		log.debug(format("Processing business event: Sequence: %s; ValueEvent: %s", sequence, event.getValue()));
		eventProcessor.process(new ResponseDTO(requestDTO, dispatchRequest(requestDTO.request)));
		log.debug("Request processed, response sent");
	}
	
	Response dispatchRequest(Request request) {
		if (request instanceof Order) {
			return orderProcessor.order((Order)request);
		} else if (request instanceof MarketRegistrationRequest) {
			return orderProcessor.registerMarket((MarketRegistrationRequest) request);
		} else if (request instanceof MarketPricesRequest) {
			return orderProcessor.marketPrices((MarketPricesRequest) request);
		} else if (request instanceof MarketsRequest) {
			return orderProcessor.availableMarkets((MarketsRequest) request);
		} else if (request instanceof AccountStateRequest) {
			return loginProcessor.accountState((AccountStateRequest) request);
		} else if (request instanceof Login) {
			return loginProcessor.login((Login) request);
		} else if (request instanceof UserRegistrationRequest) {
			return loginProcessor.registerUser((UserRegistrationRequest) request);
		} else if (request instanceof AddCreditRequest) {
			return loginProcessor.addCredit((AddCreditRequest) request);
		} else if (request instanceof WithdrawRequest) {
			return loginProcessor.withdraw((WithdrawRequest) request);
		} else {
			throw new IllegalArgumentException("request not supported: " + request);
		}
	}

	public BusinessProcessor withLoginProcessor(LoginProcessor loginProcessor) {
		this.loginProcessor = loginProcessor;
		return this;
	}

	public BusinessProcessor withOrderProcessor(OrderProcessor orderProcessor) {
		this.orderProcessor = orderProcessor;
		return this;
	}
}