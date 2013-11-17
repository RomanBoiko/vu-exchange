package vu.exchange;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

import vu.exchange.Order.Currency;

import com.google.common.collect.ImmutableMap;

public abstract class Response {
	private static final JsonConverter<Response> JSON_CONVERTER = new JsonConverter<Response>(new ImmutableMap.Builder<String, Class<? extends Response>>()
			.put(OrderSubmitResult.class.getSimpleName(), OrderSubmitResult.class)
			.put(LoginResult.class.getSimpleName(), LoginResult.class)
			.put(AccountState.class.getSimpleName(), AccountState.class)
			.put(Markets.class.getSimpleName(), Markets.class)
			.put(MarketPrices.class.getSimpleName(), MarketPrices.class)
			.put(MarketRegistrationResult.class.getSimpleName(), MarketRegistrationResult.class)
			.put(UserRegistrationResult.class.getSimpleName(), UserRegistrationResult.class)
			.put(AddCreditResult.class.getSimpleName(), AddCreditResult.class)
			.put(WithdrawResult.class.getSimpleName(), WithdrawResult.class)
			.put(InvalidSessionResponse.class.getSimpleName(), InvalidSessionResponse.class).build());

	static Response fromJson(String json) throws Exception {
		return JSON_CONVERTER.fromJson(json);
	}

	public String toJson() throws Exception {
		return JSON_CONVERTER.toJson(this);
	}
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
	public BigDecimal credit = BigDecimal.ZERO;
	public BigDecimal exposure = BigDecimal.ZERO;
	public Order.Currency currency = Currency.GBP;
	AccountState withCredit(BigDecimal credit) {this.credit = credit; return this;}
	AccountState withExposure(BigDecimal exposure) {this.exposure = exposure; return this;}
	AccountState withCurrency(Order.Currency currency) {this.currency = currency; return this;}
}

class OrderSubmitResult extends Response {
	enum OrderStatus { ACCEPTED, REJECTED }
	public String type = this.getClass().getSimpleName();
	public String orderId;
	public OrderStatus status;
	OrderSubmitResult withOrderId(String orderId) {this.orderId = orderId; return this;}
	OrderSubmitResult withStatus(OrderStatus status) {this.status = status; return this;}
}

class Markets extends Response {
	public String type = this.getClass().getSimpleName();
	public List<MarketDetails> markets = new LinkedList<MarketDetails>();
	public Markets addMarket(MarketDetails details) {
		this.markets.add(details);
		return this;
	}

	static class MarketDetails {
		public Long id;
		public String name;
		MarketDetails withId(Long id) {this.id = id; return this;}
		MarketDetails withName(String name) {this.name = name; return this;}
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
		public BigDecimal price;
		public BigDecimal quantity;
		public ProductUnit(BigDecimal price, BigDecimal quantity) {
			this.price = price;
			this.quantity = quantity;
		}
		public ProductUnit() { }
	}
}

class MarketRegistrationResult extends Response {
	enum MarketRegistrationStatus { REGISTERED, UPDATED }
	public String type = this.getClass().getSimpleName();
	public MarketRegistrationStatus registrationStatus;
	public Long id;

	MarketRegistrationResult withStatus(MarketRegistrationStatus registrationStatus) {
		this.registrationStatus = registrationStatus;
		return this;
	}

	public MarketRegistrationResult withId(long id) {
		this.id = id;
		return this;
	}
}

class UserRegistrationResult extends Response {
	enum UserRegistrationStatus { REGISTERED, PASSWORD_UPDATED, UNCHANGED  }
	public String type = this.getClass().getSimpleName();
	public UserRegistrationStatus registrationStatus;
	public String email;

	UserRegistrationResult withStatus(UserRegistrationStatus registrationStatus) {
		this.registrationStatus = registrationStatus;
		return this;
	}
	UserRegistrationResult withEmail(String email) {
		this.email = email;
		return this;
	}
}

class AddCreditResult extends Response {
	enum AddCreditStatus { SUCCESS }
	public String type = this.getClass().getSimpleName();
	public AddCreditStatus status = AddCreditStatus.SUCCESS;
	public BigDecimal amount = BigDecimal.valueOf(0);
	AddCreditResult withAmount(BigDecimal amount) {this.amount = amount; return this;}
}

class WithdrawResult extends Response {
	enum WithdrawStatus { SUCCESS, FAILURE_ACCOUNT_CREDIT_LOW }
	public String type = this.getClass().getSimpleName();
	public WithdrawStatus status = WithdrawStatus.SUCCESS;
	public BigDecimal amount = BigDecimal.valueOf(0);
	WithdrawResult withAmount(BigDecimal amount) {this.amount = amount; return this;}
	WithdrawResult withWithdrawStatus(WithdrawStatus status) {this.status = status; return this;}
}

class InvalidSessionResponse extends Response {
	public String type = this.getClass().getSimpleName();
}