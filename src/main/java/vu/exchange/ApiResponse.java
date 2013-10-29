package vu.exchange;

import java.math.BigDecimal;

import com.google.common.collect.ImmutableMap;

public abstract class ApiResponse {
	private static final JsonConverter<ApiResponse> JSON_CONVERTER = new JsonConverter<ApiResponse>(ImmutableMap.of(
			OrderSubmitResult.class.getSimpleName(), OrderSubmitResult.class,
			LoginResult.class.getSimpleName(), LoginResult.class,
			AccountState.class.getSimpleName(), AccountState.class));

	static ApiResponse fromJson(String json) throws Exception {
		return JSON_CONVERTER.fromJson(json);
	}

	public String toJson() throws Exception {
		return JSON_CONVERTER.toJson(this);
	}
}

class OrderSubmitResult extends ApiResponse {
	enum OrderStatus {
		ACCEPTED, REJECTED;
	}
	public String type = this.getClass().getSimpleName();
	public String orderId;
	public OrderStatus status;
	OrderSubmitResult withOrderId(String orderId) {this.orderId = orderId; return this;}
	OrderSubmitResult withStatus(OrderStatus status) {this.status = status; return this;}
}


class LoginResult extends ApiResponse {
	enum LoginStatus {
		OK, NO_SUCH_USER, WRONG_PASSWORD, ALREADY_LOGGED_IN
	}
	public String type = this.getClass().getSimpleName();
	public String sessionId;
	public LoginStatus status;
	LoginResult withSessionId(String sessionId) {this.sessionId = sessionId; return this;}
	LoginResult withStatus(LoginStatus status) {this.status = status; return this;}
}

class AccountState extends ApiResponse {
	public String type = this.getClass().getSimpleName();
	public BigDecimal amount;
	public BigDecimal exposure;
	public Order.Currency currency;
	AccountState withAmount(BigDecimal amount) {this.amount = amount; return this;}
	AccountState withExposure(BigDecimal exposure) {this.exposure = exposure; return this;}
	AccountState withCurrency(Order.Currency currency) {this.currency = currency; return this;}
}
