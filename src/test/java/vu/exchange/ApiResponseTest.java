package vu.exchange;

import static java.math.BigDecimal.valueOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import vu.exchange.LoginResult.LoginStatus;
import vu.exchange.OrderSubmitResult.OrderStatus;

public class ApiResponseTest {

	@Test
	public void shouldSerializeObject() throws Exception {
		assertThat(new ApiResponse().toJson(
					new OrderSubmitResult().withOrderId("111").withStatus(OrderStatus.ACCEPTED)),
					is("{\"type\":\"OrderSubmitResult\",\"orderId\":\"111\",\"status\":\"ACCEPTED\"}"));
	}

	@Test
	public void shouldSerializeLoginResult() throws Exception {
		assertThat(new ApiResponse().toJson(
					new LoginResult().withSessionId("111").withStatus(LoginStatus.WRONG_PASSWORD)),
					is("{\"type\":\"LoginResult\",\"sessionId\":\"111\",\"status\":\"WRONG_PASSWORD\"}"));
	}

	@Test
	public void shouldSerializeAccountState() throws Exception {
		assertThat(new ApiResponse().toJson(
					new AccountState().withAmount(valueOf(13.4)).withExposure(valueOf(3.5)).withCurrency(Order.Currency.GBP)),
					is("{\"type\":\"AccountState\",\"amount\":13.4,\"exposure\":3.5,\"currency\":\"GBP\"}"));
	}

}
