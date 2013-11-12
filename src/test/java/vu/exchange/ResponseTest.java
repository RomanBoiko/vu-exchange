package vu.exchange;

import static java.math.BigDecimal.valueOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;

import org.junit.Test;

import vu.exchange.LoginResult.LoginStatus;
import vu.exchange.MarketPrices.ProductUnit;
import vu.exchange.MarketRegistrationResult.MarketRegistrationStatus;
import vu.exchange.OrderSubmitResult.OrderStatus;
import vu.exchange.UserRegistrationResult.UserRegistrationStatus;

public class ResponseTest {

	@Test
	public void shouldSerializeObject() throws Exception {
		assertThat(new OrderSubmitResult().withOrderId("111").withStatus(OrderStatus.ACCEPTED).toJson(),
					is("{\"type\":\"OrderSubmitResult\",\"orderId\":\"111\",\"status\":\"ACCEPTED\"}"));
	}

	@Test
	public void shouldSerializeLoginResult() throws Exception {
		assertThat(new LoginResult().withSessionId("111").withStatus(LoginStatus.WRONG_PASSWORD).toJson(),
					is("{\"type\":\"LoginResult\",\"sessionId\":\"111\",\"status\":\"WRONG_PASSWORD\"}"));
	}

	@Test
	public void shouldSerializeAccountState() throws Exception {
		assertThat(new AccountState().withAmount(valueOf(13.4)).withExposure(valueOf(3.5)).withCurrency(Order.Currency.GBP).toJson(),
					is("{\"type\":\"AccountState\",\"amount\":13.4,\"exposure\":3.5,\"currency\":\"GBP\"}"));
	}

	@Test
	public void shouldSerializeOrderSubmitResult() throws Exception {
		assertThat(new OrderSubmitResult().withOrderId("iddddd").withStatus(OrderStatus.ACCEPTED).toJson(),
					is("{\"type\":\"OrderSubmitResult\",\"orderId\":\"iddddd\",\"status\":\"ACCEPTED\"}"));
	}

	@Test
	public void shouldSerializeMarkets() throws Exception {
		assertThat(new Markets()
				.addMarket(new Markets.MarketDetails().withId(111L).withName("mname1"))
				.addMarket(new Markets.MarketDetails().withId(222L).withName("mname2")).toJson(),
					is("{\"type\":\"Markets\",\"markets\":[{\"id\":111,\"name\":\"mname1\"},{\"id\":222,\"name\":\"mname2\"}]}"));
	}

	@Test
	public void shouldSerializeMarketPrices() throws Exception {
		assertThat(new MarketPrices()
				.addBid(new ProductUnit(BigDecimal.valueOf(21.1), BigDecimal.valueOf(22.2)))
				.addBid(new ProductUnit(BigDecimal.valueOf(23.3), BigDecimal.valueOf(24.4)))
				.addOffer(new ProductUnit(BigDecimal.valueOf(25.5), BigDecimal.valueOf(26.6)))
				.toJson(),
					is("{\"type\":\"MarketPrices\",\"marketId\":null,\"bids\":[{\"price\":21.1,\"quantity\":22.2},{\"price\":23.3,\"quantity\":24.4}],\"offers\":[{\"price\":25.5,\"quantity\":26.6}]}"));
	}

	@Test
	public void shouldSerializeMarketRegistrationResult() throws Exception {
		assertThat(new MarketRegistrationResult()
				.withStatus(MarketRegistrationStatus.REGISTERED)
				.withId(111L)
				.toJson(),
					is("{\"type\":\"MarketRegistrationResult\",\"registrationStatus\":\"REGISTERED\",\"id\":111}"));
	}

	@Test
	public void shouldSerializeUserRegistrationResult() throws Exception {
		assertThat(new UserRegistrationResult()
				.withStatus(UserRegistrationStatus.REGISTERED)
				.withEmail("some@com")
				.toJson(),
					is("{\"type\":\"UserRegistrationResult\",\"registrationStatus\":\"REGISTERED\",\"email\":\"some@com\"}"));
	}
}
