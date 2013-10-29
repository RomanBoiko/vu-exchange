package vu.exchange;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Random;

import com.google.common.collect.ImmutableMap;

public abstract class ApiRequest {
	static final int ID_MAX_PREFIX_LENGTH = 5;
	static final Integer ID_PREFIX_LIMIT = Math.power(10L, ID_MAX_PREFIX_LENGTH).intValue();
	
	static final int ID_SUFFIX_LENGTH = 13;
	static final Long SUFFIX_MULTIPLIER = Math.power(10L, ID_SUFFIX_LENGTH);


	static long requestId(Date timestamp) {
		return (new Random().nextInt(ID_PREFIX_LIMIT) * SUFFIX_MULTIPLIER) + timestamp.getTime();
	}

	private static final JsonConverter<ApiRequest> JSON_CONVERTER = new JsonConverter<ApiRequest>(ImmutableMap.of(
			Order.class.getSimpleName(), Order.class,
			Login.class.getSimpleName(), Login.class,
			AccountStateRequest.class.getSimpleName(), AccountStateRequest.class));

	static ApiRequest fromJson(String json) throws Exception {
		return JSON_CONVERTER.fromJson(json);
	}

	public String toJson() throws Exception {
		return JSON_CONVERTER.toJson(this);
	}

	private Long id = requestId(new Date());
	public Long id() {return id;}

	ApiRequest withArrivalTimestamp(Date timestamp) {
		this.id = requestId(timestamp);
		return this;
	}

	Long timestamp() {
		return id % SUFFIX_MULTIPLIER;
	}
}

class Order extends ApiRequest {
	static enum Position { BUY, SELL }
	static enum Currency { GBP }

	public String type = this.getClass().getSimpleName();
	public String session = "default-session-id";
	public Long market = 0L;
	public BigDecimal price = BigDecimal.valueOf(0);
	public Currency currency = Currency.GBP;
	public BigDecimal quantity = BigDecimal.valueOf(0);
	public Position position = Position.BUY;

	Order withPosition(Position position) {this.position = position; return this;}
	Order withPrice(double price) {this.price = BigDecimal.valueOf(price); return this;}
	Order withQuantity(double quantity) {this.quantity = BigDecimal.valueOf(quantity); return this;}
	Order withMarket(long market) {this.market = market; return this;}
	Order withSession(String session) {this.session = session; return this;}
}

class Login extends ApiRequest {
	public String type = this.getClass().getSimpleName();
	public String email = "some@somewhere.com";
	public String password = "pass";

	Login withEmail(String email) {this.email = email; return this;}
	Login withPassword(String password) {this.password = password; return this;}
}

class AccountStateRequest extends ApiRequest {
	public String type = this.getClass().getSimpleName();
	public String session = "default-session-id";
	AccountStateRequest withSession(String session) {this.session = session; return this;}
}
