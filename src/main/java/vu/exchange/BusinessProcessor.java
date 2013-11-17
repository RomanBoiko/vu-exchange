package vu.exchange;

import static java.lang.String.format;


import org.apache.log4j.Logger;

import vu.exchange.LoginProcessor.UserSession;
import vu.exchange.RequestResponseRepo.RequestDTO;
import vu.exchange.RequestResponseRepo.ResponseDTO;

import com.lmax.disruptor.EventHandler;

public class BusinessProcessor implements EventHandler<ValueEvent>{
	private final Logger log = Logger.getLogger(this.getClass());
	private final DisruptorBridge eventProcessor;

	private LoginProcessor loginProcessor;
	private OrderProcessor orderProcessor;
	
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
		if (request instanceof Login) {
			return loginProcessor.login((Login) request);
		} else if(request instanceof AuthenticatedRequest) {
			UserSession session = loginProcessor.sessionDetails((AuthenticatedRequest) request);
			if(!session.isSessionValid) {
				return new InvalidSessionResponse();
			}
			if(session.hasSystemAdminPermissions) {
				if (request instanceof MarketRegistrationRequest) {
					return orderProcessor.registerMarket((MarketRegistrationRequest) request);
				} else if (request instanceof UserRegistrationRequest) {
					return loginProcessor.registerUser((UserRegistrationRequest) request);
				} else if (request instanceof AddCreditRequest) {
					return loginProcessor.addCredit((AddCreditRequest) request);
				} else if (request instanceof WithdrawRequest) {
					return loginProcessor.withdraw((WithdrawRequest) request);
				}
			}
			if (request instanceof Order) {
				return orderProcessor.order((Order)request);
			} else if (request instanceof MarketPricesRequest) {
				return orderProcessor.marketPrices((MarketPricesRequest) request);
			} else if (request instanceof MarketsRequest) {
				return orderProcessor.availableMarkets((MarketsRequest) request);
			} else if (request instanceof AccountStateRequest) {
				return loginProcessor.accountState((AccountStateRequest) request);
			}
		}

		throw new IllegalArgumentException("request not supported: " + request);
	}

	BusinessProcessor withLoginProcessor(LoginProcessor loginProcessor) {
		this.loginProcessor = loginProcessor;
		return this;
	}

	BusinessProcessor withOrderProcessor(OrderProcessor orderProcessor) {
		this.orderProcessor = orderProcessor;
		return this;
	}
}