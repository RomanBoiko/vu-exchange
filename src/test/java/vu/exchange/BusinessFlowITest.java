package vu.exchange;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;

import vu.exchange.Order.Position;

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
		MarketRegistrationResult market1RegistrationResult = (MarketRegistrationResult) getExchangeResponse(
				new MarketRegistrationRequest()
					.withId(11L)
					.withName("France Ukraine 0-1")
					.withSessionId(sysUserLoginResult.sessionId));
		MarketRegistrationResult market2RegistrationResult = (MarketRegistrationResult) getExchangeResponse(
				new MarketRegistrationRequest()
					.withId(12L)
					.withName("France Ukraine 1-1")
					.withSessionId(sysUserLoginResult.sessionId));
		UserRegistrationResult userRegistrationResult = (UserRegistrationResult) getExchangeResponse(
				new UserRegistrationRequest()
					.withEmail(EMAIL)
					.withPassword(PASSWORD)
					.withSessionId(sysUserLoginResult.sessionId));
		AddCreditResult addCreditResult = (AddCreditResult) getExchangeResponse(
				new AddCreditRequest()
					.withAmount(10.0)
					.withEmail(EMAIL)
					.withSessionId(sysUserLoginResult.sessionId));
		LoginResult clientLoginResult = (LoginResult) getExchangeResponse(
				new Login()
					.withEmail(EMAIL)
					.withPassword(PASSWORD));
		AccountState clientAccountState = (AccountState) getExchangeResponse(
				new AccountStateRequest()
				.withEmail(EMAIL)
				.withSessionId(clientLoginResult.sessionId));
		Markets markets = (Markets) getExchangeResponse(new MarketsRequest().withSessionId(clientLoginResult.sessionId));
		Long marketId = markets.markets.get(0).id;
		MarketPrices pricesBeforeOrders = (MarketPrices) getExchangeResponse(
				new MarketPricesRequest()
					.withMarketId(marketId)
					.withSessionId(clientLoginResult.sessionId));
		OrderSubmitResult order1SubmitResult = (OrderSubmitResult) getExchangeResponse(
				new Order()
				.withMarket(marketId)
				.withPosition(Position.BUY)
				.withPrice(23.3)
				.withQuantity(3)
				.withSessionId(clientLoginResult.sessionId));
		OrderSubmitResult order2SubmitResult = (OrderSubmitResult) getExchangeResponse(
				new Order()
				.withMarket(marketId)
				.withPosition(Position.BUY)
				.withPrice(25.5)
				.withQuantity(4)
				.withSessionId(clientLoginResult.sessionId));
		OrderSubmitResult order3SubmitResult = (OrderSubmitResult) getExchangeResponse(
				new Order()
				.withMarket(marketId)
				.withPosition(Position.SELL)
				.withPrice(30.1)
				.withQuantity(7)
				.withSessionId(clientLoginResult.sessionId));
		MarketPrices pricesAfterOrders = (MarketPrices) getExchangeResponse(
				new MarketPricesRequest()
					.withMarketId(marketId)
					.withSessionId(clientLoginResult.sessionId));
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