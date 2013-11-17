package vu.exchange;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.util.Date;

import org.hamcrest.Matchers;
import org.junit.Test;

import vu.exchange.Order.Currency;
import vu.exchange.Order.Position;

public class RequestTest {

	private static final String ORDER = "{\"sessionId\":\"sessionId\",\"type\":\"Order\",\"marketId\":1,\"price\":1.1,\"currency\":\"GBP\",\"quantity\":1.2,\"position\":\"SELL\"}";
	private static final String LOGIN = "{\"type\":\"Login\",\"email\":\"email@email.com\",\"password\":\"pass\"}";
	private static final String ACCOUNT_STATE_REQUEST = "{\"sessionId\":\"sessionId\",\"type\":\"AccountStateRequest\",\"email\":\"email@com\"}";
	private static final String MARKETS_REQUEST = "{\"sessionId\":\"sessionId\",\"type\":\"MarketsRequest\"}";
	private static final String MARKET_PRICES_REQUEST = "{\"sessionId\":\"sessionId\",\"type\":\"MarketPricesRequest\",\"marketId\":111}";
	private static final String MARKET_REGISTRATION_REQUEST = "{\"id\":111,\"sessionId\":\"sessionId\",\"type\":\"MarketRegistrationRequest\",\"name\":\"mname\"}";
	private static final String USER_REGISTRATION_REQUEST = "{\"sessionId\":\"sessionId\",\"type\":\"UserRegistrationRequest\",\"email\":\"email@com\",\"password\":\"secret\"}";
	private static final String ADD_CREDIT_REQUEST = "{\"sessionId\":\"sessionId\",\"type\":\"AddCreditRequest\",\"email\":\"email@com\",\"amount\":1.2}";
	private static final String WITHDRAW_REQUEST = "{\"sessionId\":\"sessionId\",\"type\":\"WithdrawRequest\",\"email\":\"email@com\",\"amount\":1.2}";
	

	@Test
	public void shouldGenerateOrderWithTimestampAsPartOfUniqueId() {
		Order order = new Order();
		order.withArrivalTimestamp(new Date());
		Long orderTimestamp = order.timestamp();
		Long orderId = order.id();
		
		assertThat(orderTimestamp.toString().length(), is(Order.ID_SUFFIX_LENGTH));
		assertThat(orderId.toString().length(), Matchers.greaterThan(Order.ID_SUFFIX_LENGTH));
		assertThat(orderId.toString().length(), Matchers.lessThanOrEqualTo(Order.ID_MAX_PREFIX_LENGTH + Order.ID_SUFFIX_LENGTH));
		assertThat(orderId.toString(), endsWith(orderTimestamp.toString()));
		assertThat(idPrefix(orderId), not(idPrefix(new Order().withArrivalTimestamp(new Date()).id())));
	}

	@Test
	public void shouldSerializeOrder() throws Exception {
		String orderRequest = new Order().withMarket(1L).withPosition(Position.SELL).withPrice(1.1).withQuantity(1.2).withSessionId("sessionId").toJson();
		assertThat(orderRequest, is(ORDER));
	}

	@Test
	public void shouldDeserializeOrder() throws Exception {
		Order order = (Order)Request.fromJson(ORDER);
		assertThat(order.marketId, is(1L));
		assertThat(order.price, is(BigDecimal.valueOf(1.1)));
		assertThat(order.currency, is(Currency.GBP));
		assertThat(order.quantity, is(BigDecimal.valueOf(1.2)));
		assertThat(order.position, is(Position.SELL));
		assertThat(order.sessionId, is("sessionId"));
	}

	@Test
	public void shouldSerializeLogin() throws Exception {
		String loginRequest = new Login().withEmail("email@email.com").withPassword("pass").toJson();
		assertThat(loginRequest, is(LOGIN));
	}

	@Test
	public void shouldDeserializeLogin() throws Exception {
		Login login = (Login)Request.fromJson(LOGIN);
		assertThat(login.email, is("email@email.com"));
		assertThat(login.password, is("pass"));
	}

	@Test
	public void shouldSerializeAccountStateRequest() throws Exception {
		assertThat(new AccountStateRequest().withEmail("email@com").withSessionId("sessionId").toJson(), is(ACCOUNT_STATE_REQUEST));
	}

	@Test
	public void shouldDeserializeAccountStateRequest() throws Exception {
		AccountStateRequest accountRequest = (AccountStateRequest)Request.fromJson(ACCOUNT_STATE_REQUEST);
		assertThat(accountRequest.sessionId, is("sessionId"));
		assertThat(accountRequest.email, is("email@com"));
	}
	
	@Test
	public void shouldSerializeMarketsRequest() throws Exception {
		assertThat(new MarketsRequest().withSessionId("sessionId").toJson(), is(MARKETS_REQUEST));
	}

	@Test
	public void shouldDeserializeMarketsRequest() throws Exception {
		assertThat(((MarketsRequest)Request.fromJson(MARKETS_REQUEST)).sessionId, is("sessionId"));
	}
	
	@Test
	public void shouldSerializeMarketPricesRequest() throws Exception {
		assertThat(new MarketPricesRequest().withMarketId(111L).withSessionId("sessionId").toJson(), is(MARKET_PRICES_REQUEST));
	}

	@Test
	public void shouldDeserializeMarketPricesRequest() throws Exception {
		assertThat(((MarketPricesRequest)Request.fromJson(MARKET_PRICES_REQUEST)).marketId, is(111L));
	}

	@Test
	public void shouldSerializeMarketDetails() throws Exception {
		assertThat(new MarketRegistrationRequest().withId(111L).withName("mname").withSessionId("sessionId").toJson(), is(MARKET_REGISTRATION_REQUEST));
	}

	@Test
	public void shouldDeserializeMarketDetails() throws Exception {
		assertThat(((MarketRegistrationRequest)Request.fromJson(MARKET_REGISTRATION_REQUEST)).name, is("mname"));
	}

	@Test
	public void shouldSerializeUserRegistrationRequest() throws Exception {
		assertThat(new UserRegistrationRequest().withEmail("email@com").withPassword("secret").withSessionId("sessionId").toJson(), is(USER_REGISTRATION_REQUEST));
	}

	@Test
	public void shouldDeserializeUserRegistrationRequest() throws Exception {
		assertThat(((UserRegistrationRequest)Request.fromJson(USER_REGISTRATION_REQUEST)).email, is("email@com"));
	}
	
	@Test
	public void shouldSerializeAddCreditRequest() throws Exception {
		assertThat(new AddCreditRequest().withEmail("email@com").withAmount(1.2).withSessionId("sessionId").toJson(), is(ADD_CREDIT_REQUEST));
	}

	@Test
	public void shouldDeserializeAddCreditRequest() throws Exception {
		assertThat(((AddCreditRequest)Request.fromJson(ADD_CREDIT_REQUEST)).amount, is(BigDecimal.valueOf(1.2)));
	}
	
	@Test
	public void shouldSerializeWithdrawRequest() throws Exception {
		assertThat(new WithdrawRequest().withEmail("email@com").withAmount(1.2).withSessionId("sessionId").toJson(), is(WITHDRAW_REQUEST));
	}

	@Test
	public void shouldDeserializeWithdrawRequest() throws Exception {
		assertThat(((WithdrawRequest)Request.fromJson(WITHDRAW_REQUEST)).amount, is(BigDecimal.valueOf(1.2)));
	}

	private String idPrefix(Long id) {
		return id.toString().substring(0, (id.toString().length() - Order.ID_SUFFIX_LENGTH));
	}
}
