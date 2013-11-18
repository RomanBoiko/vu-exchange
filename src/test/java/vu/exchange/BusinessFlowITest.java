package vu.exchange;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static vu.exchange.LoginResult.LoginStatus;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;

import vu.exchange.AddCreditResult.AddCreditStatus;
import vu.exchange.MarketRegistrationResult.MarketRegistrationStatus;
import vu.exchange.Order.Position;
import vu.exchange.OrderSubmitResult.OrderStatus;
import vu.exchange.UserRegistrationResult.UserRegistrationStatus;

public class BusinessFlowITest {

	private static final AppContext APP_CONTEXT = new AppContext(new File("env/boikoro/app.properties"));
	private static final String EMAIL = "user1@smarkets.com";
	private static final String PASSWORD = "passOfUser1";
	private Exchange exchange;
	private IBlockingConnection clientConnection;

	@Before
	public void startExchange() throws Exception {
		exchange = new Exchange(APP_CONTEXT);
		exchange.start();
		clientConnection = new BlockingConnection("localhost", APP_CONTEXT.apiTcpPort());
	}

	@After
	public void stopExchange() throws Exception {
		clientConnection.close();
		exchange.stop();
	}

	@Test
	public void shouldSupportAllTypesOfMessagesInBusinessFlow() throws Exception {
		LoginResult sysUserLoginResult = (LoginResult) getExchangeResponse(
				new Login()
					.withEmail(APP_CONTEXT.systemUserName())
					.withPassword(APP_CONTEXT.systemUserPassword()));
		assertThat(sysUserLoginResult.status, is(LoginStatus.OK));

		String systemUserSessionId = sysUserLoginResult.sessionId;

		MarketRegistrationResult market1RegistrationResult = (MarketRegistrationResult) getExchangeResponse(
				new MarketRegistrationRequest()
					.withId(11L)
					.withName("France Ukraine 0-1")
					.withSessionId(systemUserSessionId));
		assertThat(market1RegistrationResult.registrationStatus, is(MarketRegistrationStatus.REGISTERED));

		MarketRegistrationResult market2RegistrationResult = (MarketRegistrationResult) getExchangeResponse(
				new MarketRegistrationRequest()
					.withId(12L)
					.withName("France Ukraine 1-1")
					.withSessionId(systemUserSessionId));
		assertThat(market2RegistrationResult.registrationStatus, is(MarketRegistrationStatus.REGISTERED));

		UserRegistrationResult userRegistrationResult = (UserRegistrationResult) getExchangeResponse(
				new UserRegistrationRequest()
					.withEmail(EMAIL)
					.withPassword(PASSWORD)
					.withSessionId(systemUserSessionId));
		assertThat(userRegistrationResult.registrationStatus, is(UserRegistrationStatus.REGISTERED));

		AddCreditResult addCreditResult = (AddCreditResult) getExchangeResponse(
				new AddCreditRequest()
					.withAmount(10.0)
					.withEmail(EMAIL)
					.withSessionId(systemUserSessionId));
		assertThat(addCreditResult.status, is(AddCreditStatus.SUCCESS));

		LoginResult clientLoginResult = (LoginResult) getExchangeResponse(
				new Login()
					.withEmail(EMAIL)
					.withPassword(PASSWORD));

		String clientSessionId = clientLoginResult.sessionId;

		AccountState clientAccountState = (AccountState) getExchangeResponse(
				new AccountStateRequest()
				.withEmail(EMAIL)
				.withSessionId(clientSessionId));
		assertThat(clientAccountState.credit, is(BigDecimal.valueOf(10.0)));

		Markets markets = (Markets) getExchangeResponse(new MarketsRequest().withSessionId(clientSessionId));
		assertThat(markets.markets.size(), is(2));

		Long market1Id = markets.markets.get(0).id;

		MarketPrices pricesBeforeOrders = (MarketPrices) getExchangeResponse(
				new MarketPricesRequest()
					.withMarketId(market1Id)
					.withSessionId(clientSessionId));
		assertThat(pricesBeforeOrders.bids.size(), is(0));
		assertThat(pricesBeforeOrders.offers.size(), is(0));
		
		OrderSubmitResult order1SubmitResult = (OrderSubmitResult) getExchangeResponse(
				new Order()
				.withMarket(market1Id)
				.withPosition(Position.BUY)
				.withPrice(23.3)
				.withQuantity(3)
				.withSessionId(clientSessionId));
		assertThat(order1SubmitResult.status, is(OrderStatus.ACCEPTED));

		OrderSubmitResult order2SubmitResult = (OrderSubmitResult) getExchangeResponse(
				new Order()
				.withMarket(market1Id)
				.withPosition(Position.BUY)
				.withPrice(25.5)
				.withQuantity(4)
				.withSessionId(clientSessionId));
		assertThat(order2SubmitResult.status, is(OrderStatus.ACCEPTED));

		OrderSubmitResult order3SubmitResult = (OrderSubmitResult) getExchangeResponse(
				new Order()
				.withMarket(market1Id)
				.withPosition(Position.SELL)
				.withPrice(30.1)
				.withQuantity(7)
				.withSessionId(clientSessionId));
		assertThat(order3SubmitResult.status, is(OrderStatus.ACCEPTED));

		MarketPrices pricesAfterOrders = (MarketPrices) getExchangeResponse(
				new MarketPricesRequest()
					.withMarketId(market1Id)
					.withSessionId(clientSessionId));
		assertThat(pricesAfterOrders.bids.size(), is(2));
		assertThat(pricesAfterOrders.offers.size(), is(1));
	}

	private Response getExchangeResponse(Request request)
			throws Exception {
		String response = sendMessage(request.toJson());
		Response apiResponse = Response.fromJson(response);
		return apiResponse;
	}

	private String sendMessage(String request) throws IOException {
		System.out.println("==>Request:  " + request);
		clientConnection.write(request + "\n");
		String response = clientConnection.readStringByDelimiter("\n");
		System.out.println("==>Response: " + response);
		return response;
	}
}