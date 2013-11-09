package vu.exchange;

import static java.lang.String.format;
import static vu.exchange.LoginResult.LoginStatus.ALREADY_LOGGED_IN;
import static vu.exchange.LoginResult.LoginStatus.NO_SUCH_USER;
import static vu.exchange.LoginResult.LoginStatus.OK;
import static vu.exchange.LoginResult.LoginStatus.WRONG_PASSWORD;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import vu.exchange.OrderSubmitResult.OrderStatus;
import vu.exchange.RequestResponseRepo.RequestDTO;
import vu.exchange.RequestResponseRepo.ResponseDTO;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.lmax.disruptor.EventHandler;

public class BusinessProcessor implements EventHandler<ValueEvent>{
	private final Logger log = Logger.getLogger(this.getClass());
	private final DisruptorBridge eventProcessor;

	LoginProcessor loginProcessor = new LoginProcessor();
	
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
			return order((Order)request);
		} else if (request instanceof Login) {
			return loginProcessor.login((Login) request);
		} else {
			throw new IllegalArgumentException("request not supported: " + request);
		}

	}


	private OrderSubmitResult order(Order order) {
		return new OrderSubmitResult().withOrderId(order.id().toString()).withStatus(OrderStatus.ACCEPTED);
	}
}


class LoginProcessor {
	private final Map<String, String> email2Password = new HashMap<String, String>(
			new ImmutableMap.Builder<String, String>().put("user1@smarkets.com", "pass1").build());
	private BiMap<String, String> sessionToLogin = HashBiMap.create();

	void registerUser(String email, String password) {
		email2Password.put(email, password);
	}

	LoginResult login(Login login) {
		String password = email2Password.get(login.email);
		if(null == password) {
			return new LoginResult().withStatus(NO_SUCH_USER);
		} else if (password.equals(login.password)) {
			return onCorrectCredentials(login);
		} else {
			return new LoginResult().withStatus(WRONG_PASSWORD);
		}
	}

	private LoginResult onCorrectCredentials(Login login) {
		String existingSessionId = sessionToLogin.inverse().get(login.email);
		if(existingSessionId == null) {
			return loggedInSuccessfuly(login);
		} else {
			return new LoginResult().withStatus(ALREADY_LOGGED_IN).withSessionId(existingSessionId);
		}
	}

	private LoginResult loggedInSuccessfuly(Login login) {
		String sessionId = UUID.randomUUID().toString();
		sessionToLogin.put(sessionId, login.email);
		return new LoginResult().withStatus(OK).withSessionId(sessionId);
	}
}