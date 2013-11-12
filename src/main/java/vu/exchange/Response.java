package vu.exchange;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.ImmutableMap;

public abstract class Response {
	private static final JsonConverter<Response> JSON_CONVERTER = new JsonConverter<Response>(ImmutableMap.of(
			OrderSubmitResult.class.getSimpleName(), OrderSubmitResult.class,
			LoginResult.class.getSimpleName(), LoginResult.class,
			AccountState.class.getSimpleName(), AccountState.class));

	static Response fromJson(String json) throws Exception {
		return JSON_CONVERTER.fromJson(json);
	}

	public String toJson() throws Exception {
		return JSON_CONVERTER.toJson(this);
	}
}

class OrderSubmitResult extends Response {
	enum OrderStatus { ACCEPTED, REJECTED }
	public String type = this.getClass().getSimpleName();
	public String orderId;
	public OrderStatus status;
	OrderSubmitResult withOrderId(String orderId) {this.orderId = orderId; return this;}
	OrderSubmitResult withStatus(OrderStatus status) {this.status = status; return this;}
}


class LoginResult extends Response {
	enum LoginStatus { OK, NO_SUCH_USER, WRONG_PASSWORD, ALREADY_LOGGED_IN }
	public String type = this.getClass().getSimpleName();
	public String sessionId;
	public LoginStatus status;
	LoginResult withSessionId(String sessionId) {this.sessionId = sessionId; return this;}
	LoginResult withStatus(LoginStatus status) {this.status = status; return this;}
}

class AccountState extends Response {
	public String type = this.getClass().getSimpleName();
	public BigDecimal amount;
	public BigDecimal exposure;
	public Order.Currency currency;
	AccountState withAmount(BigDecimal amount) {this.amount = amount; return this;}
	AccountState withExposure(BigDecimal exposure) {this.exposure = exposure; return this;}
	AccountState withCurrency(Order.Currency currency) {this.currency = currency; return this;}
}

class Markets extends Response {
	public String type = this.getClass().getSimpleName();
	public List<MarketDetails> markets = new LinkedList<MarketDetails>();
	public Markets addMarket(MarketDetails details) {
		this.markets.add(details);
		return this;
	}
}
class MarketPrices extends Response {
	public String type = this.getClass().getSimpleName();
	public Long marketId;
	public List<ProductUnit> bids = new LinkedList<ProductUnit>();
	public List<ProductUnit> offers = new LinkedList<ProductUnit>();

	public MarketPrices withMarket(Long marketId) {
		this.marketId = marketId;
		return this;
	}

	public MarketPrices addBid(ProductUnit bid) {
		this.bids.add(bid);
		return this;
	}
	public MarketPrices addOffer(ProductUnit offer) {
		this.offers.add(offer);
		return this;
	}
	
	static class ProductUnit {
		public final BigDecimal price;
		public final BigDecimal quantity;
		public ProductUnit(BigDecimal price, BigDecimal quantity) {
			this.price = price;
			this.quantity = quantity;
		}
	}
}

class MarketRegistrationResult extends Response {
	enum MarketRegistrationStatus { REGISTERED, UPDATED }
	public String type = this.getClass().getSimpleName();
	public MarketRegistrationStatus registrationStatus;

	MarketRegistrationResult withStatus(MarketRegistrationStatus registrationStatus) {
		this.registrationStatus = registrationStatus;
		return this;
	}
}

class UserRegistrationResult extends Response {
	enum UserRegistrationStatus { REGISTERED, PASSWORD_UPDATED, BALANCE_UPDATED }
	public String type = this.getClass().getSimpleName();
	public UserRegistrationStatus registrationStatus;
	UserRegistrationResult withStatus(UserRegistrationStatus registrationStatus) {
		this.registrationStatus = registrationStatus;
		return this;
	}
}