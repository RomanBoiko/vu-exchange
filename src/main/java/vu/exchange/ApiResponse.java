package vu.exchange;

import java.math.BigDecimal;

import org.codehaus.jackson.map.ObjectMapper;

public class ApiResponse {
	private final ObjectMapper objectMapper = new ObjectMapper();

	public String toJson(Object response) throws Exception {
		return objectMapper.writeValueAsString(response);
	}
}

class OrderSubmitResult {
	enum OrderStatus {
		ACCEPTED, REJECTED;
	}
	public String type = this.getClass().getSimpleName();
	public String orderId;
	public OrderStatus status;
	OrderSubmitResult withOrderId(String orderId) {this.orderId = orderId; return this;}
	OrderSubmitResult withStatus(OrderStatus status) {this.status = status; return this;}
}


class LoginResult {
	enum LoginStatus {
		OK, NO_SUCH_USER, WRONG_PASSWORD, ALREADY_LOGGED_IN
	}
	public String type = this.getClass().getSimpleName();
	public String sessionId;
	public LoginStatus status;
	LoginResult withSessionId(String sessionId) {this.sessionId = sessionId; return this;}
	LoginResult withStatus(LoginStatus status) {this.status = status; return this;}
}

class AccountState {
	public String type = this.getClass().getSimpleName();
	public BigDecimal amount;
	public BigDecimal exposure;
	public Order.Currency currency;
	AccountState withAmount(BigDecimal amount) {this.amount = amount; return this;}
	AccountState withExposure(BigDecimal exposure) {this.exposure = exposure; return this;}
	AccountState withCurrency(Order.Currency currency) {this.currency = currency; return this;}
}
