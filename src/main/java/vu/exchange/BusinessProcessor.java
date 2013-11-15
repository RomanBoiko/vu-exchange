package vu.exchange;

import static java.lang.String.format;
import static vu.exchange.LoginResult.LoginStatus.ALREADY_LOGGED_IN;
import static vu.exchange.LoginResult.LoginStatus.NO_SUCH_USER;
import static vu.exchange.LoginResult.LoginStatus.OK;
import static vu.exchange.LoginResult.LoginStatus.WRONG_PASSWORD;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.log4j.Logger;

import vu.exchange.MarketRegistrationResult.MarketRegistrationStatus;
import vu.exchange.Markets.MarketDetails;
import vu.exchange.OrderSubmitResult.OrderStatus;
import vu.exchange.RequestResponseRepo.RequestDTO;
import vu.exchange.RequestResponseRepo.ResponseDTO;
import vu.exchange.UserRegistrationResult.UserRegistrationStatus;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
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
		} else if (request instanceof Login) {
			return loginProcessor.login((Login) request);
		} else {
			throw new IllegalArgumentException("request not supported: " + request);
		}
	}
}

class OrderProcessor {
	private final Map<Long, Book> marketToBook = new HashMap<Long, OrderProcessor.Book>();
	private final Map<Long, MarketDetails> marketIdToMarketDetails = new HashMap<Long, MarketDetails>();

	MarketRegistrationResult registerMarket(MarketRegistrationRequest details) {
		this.marketIdToMarketDetails.put(details.id, new MarketDetails().withId(details.id).withName(details.name));
		if(!this.marketToBook.containsKey(details.id)) {
			this.marketToBook.put(details.id, new Book());
			return new MarketRegistrationResult().withStatus(MarketRegistrationStatus.REGISTERED).withId(details.id);
		}
		return new MarketRegistrationResult().withStatus(MarketRegistrationStatus.UPDATED).withId(details.id);
	}

	Markets availableMarkets(MarketsRequest marketsRequest) {
		Markets markets = new Markets();
		for (Markets.MarketDetails details: marketIdToMarketDetails.values()) {
			markets.addMarket(details);
		}
		return markets;
	}

	MarketPrices marketPrices(MarketPricesRequest pricesRequest) {
		MarketPrices marketPrices =  new MarketPrices().withMarket(pricesRequest.marketId);
		Book book = marketToBook.get(pricesRequest.marketId);
		if(null != book) {
			for(List<Order> bidsList: book.bids.values()) {
				for(Order bid: bidsList) {
					marketPrices.addBid(new MarketPrices.ProductUnit(bid.price, bid.quantity));
				}
			}
			for(List<Order> offersList: book.offers.values()) {
				for(Order offer: offersList) {
					marketPrices.addOffer(new MarketPrices.ProductUnit(offer.price, offer.quantity));
				}
			}
		}
		return marketPrices;
	}

	OrderSubmitResult order(Order order) {
		if(!this.marketToBook.containsKey(order.marketId)) {
			return new OrderSubmitResult().withOrderId(order.id().toString()).withStatus(OrderStatus.REJECTED);
		}
		this.marketToBook.get(order.marketId).addOrder(order);
		return new OrderSubmitResult().withOrderId(order.id().toString()).withStatus(OrderStatus.ACCEPTED);
	}

	private static class Book {

		private TreeMap<BigDecimal, List<Order>> offers = new TreeMap<BigDecimal, List<Order>>();
		private TreeMap<BigDecimal, List<Order>> bids = new TreeMap<BigDecimal, List<Order>>(new DescComparator());
		void addOrder(Order order) {
			if(order.position.equals(Order.Position.BUY)) {
				putOrder(order, bids);
			} else {
				putOrder(order, offers);
			}
		}

		private void putOrder(Order order, Map<BigDecimal, List<Order>> ordersMap) {
			List<Order> orders = ordersMap.get(order.price);
			if (orders == null) {
				orders = new LinkedList<Order>();
				ordersMap.put(order.price, orders);
			}
			orders.add(order);
		}

		private static final class DescComparator implements Comparator<BigDecimal> {
			public int compare(BigDecimal o1, BigDecimal o2) {
				return o2.compareTo(o1);
			}
		}
	}
}


class LoginProcessor {
	private static class UserDetails {
		public String password;
		UserDetails withPassword(String password) {this.password = password; return this; }
	}

	private final Map<String, UserDetails> email2Details = new HashMap<String, UserDetails>(
			new ImmutableMap.Builder<String, UserDetails>()
				.put("user1@smarkets.com", new UserDetails().withPassword("pass1"))
				.build());
	private BiMap<String, String> sessionToEmail = HashBiMap.create();

	UserRegistrationResult registerUser(UserRegistrationRequest userRegistrationRequest) {
		UserDetails existingUserDetails = email2Details.get(userRegistrationRequest.email);
		if(null == existingUserDetails) {
			email2Details.put(userRegistrationRequest.email, new UserDetails().withPassword(userRegistrationRequest.password));
			return registrationResult(userRegistrationRequest, UserRegistrationStatus.REGISTERED);
		} else if(! existingUserDetails.password.equals(userRegistrationRequest.password)){
			existingUserDetails.withPassword(userRegistrationRequest.password);
			return registrationResult(userRegistrationRequest, UserRegistrationStatus.PASSWORD_UPDATED);
		} else {
			return registrationResult(userRegistrationRequest, UserRegistrationStatus.UNCHANGED);
		}
	}

	private UserRegistrationResult registrationResult(UserRegistrationRequest userRegistrationRequest, UserRegistrationStatus status) {
		return new UserRegistrationResult().withStatus(status).withEmail(userRegistrationRequest.email);
	}

	LoginResult login(Login login) {
		UserDetails existingUserDetails = email2Details.get(login.email);
		if(null == existingUserDetails) {
			return new LoginResult().withStatus(NO_SUCH_USER);
		} else if (existingUserDetails.password.equals(login.password)) {
			return onCorrectCredentials(login);
		} else {
			return new LoginResult().withStatus(WRONG_PASSWORD);
		}
	}

	private LoginResult onCorrectCredentials(Login login) {
		String existingSessionId = sessionToEmail.inverse().get(login.email);
		if(existingSessionId == null) {
			return loggedInSuccessfuly(login);
		} else {
			return new LoginResult().withStatus(ALREADY_LOGGED_IN).withSessionId(existingSessionId);
		}
	}

	private LoginResult loggedInSuccessfuly(Login login) {
		String sessionId = UUID.randomUUID().toString();
		sessionToEmail.put(sessionId, login.email);
		return new LoginResult().withStatus(OK).withSessionId(sessionId);
	}
}