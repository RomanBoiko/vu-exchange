package vu.exchange;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Random;

import com.google.common.collect.ImmutableMap;

public abstract class Request {
	static final int ID_MAX_PREFIX_LENGTH = 5;
	static final Integer ID_PREFIX_LIMIT = Math.power(10L, ID_MAX_PREFIX_LENGTH).intValue();
	
	static final int ID_SUFFIX_LENGTH = 13;
	static final Long SUFFIX_MULTIPLIER = Math.power(10L, ID_SUFFIX_LENGTH);


	static long requestId(Date timestamp) {
		return (new Random().nextInt(ID_PREFIX_LIMIT) * SUFFIX_MULTIPLIER) + timestamp.getTime();
	}

	private static final JsonConverter<Request> JSON_CONVERTER = new JsonConverter<Request>(ImmutableMap.of(
			Order.class.getSimpleName(), Order.class,
			Login.class.getSimpleName(), Login.class,
			AccountStateRequest.class.getSimpleName(), AccountStateRequest.class));

	static Request fromJson(String json) throws Exception {
		return JSON_CONVERTER.fromJson(json);
	}

	public String toJson() throws Exception {
		return JSON_CONVERTER.toJson(this);
	}

	private Long id = requestId(new Date());
	public Long id() {return id;}

	Request withArrivalTimestamp(Date timestamp) {
		this.id = requestId(timestamp);
		return this;
	}

	Long timestamp() {
		return id % SUFFIX_MULTIPLIER;
	}
}

class Login extends Request {
	public String type = this.getClass().getSimpleName();
	public String email = "some@somewhere.com";
	public String password = "pass";

	Login withEmail(String email) {this.email = email; return this;}
	Login withPassword(String password) {this.password = password; return this;}
}

abstract class AuthenticatedRequest extends Request {
	public String sessionId = "default-session-id";

	AuthenticatedRequest withSessionId(String sessionId) {
		this.sessionId = sessionId;
		return this;
	}
}

class Order extends AuthenticatedRequest {
	static enum Position { BUY, SELL }
	static enum Currency { GBP }
	
	public String type = this.getClass().getSimpleName();
	public Long marketId = 0L;
	public BigDecimal price = BigDecimal.valueOf(0);
	public Currency currency = Currency.GBP;
	public BigDecimal quantity = BigDecimal.valueOf(0);
	public Position position = Position.BUY;
	
	Order withPosition(Position position) {this.position = position; return this;}
	Order withPrice(double price) {this.price = BigDecimal.valueOf(price); return this;}
	Order withQuantity(double quantity) {this.quantity = BigDecimal.valueOf(quantity); return this;}
	Order withMarket(long marketId) {this.marketId = marketId; return this;}
}

class AccountStateRequest extends AuthenticatedRequest {
	public String type = this.getClass().getSimpleName();
}

class MarketsRequest extends AuthenticatedRequest {
	public String type = this.getClass().getSimpleName();
}

class MarketPricesRequest extends Request {
	public String type = this.getClass().getSimpleName();
	public Long marketId;
}

class MarketDetails extends Request {
	public String type = this.getClass().getSimpleName();
	public Long id;
}

class UserRegistrationRequest extends Request {
	public String type = this.getClass().getSimpleName();
	public String email;
	public String password;
	UserRegistrationRequest withEmail(String email) {this.email = email; return this;}
	UserRegistrationRequest withPassword(String password) {this.password = password; return this;}
}