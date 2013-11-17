package vu.exchange;

import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import vu.exchange.LoginProcessor.UserSession;

@RunWith(MockitoJUnitRunner.class)
public class BusinessProcessorTest {

	BusinessProcessor businessProcessor;
	
	@Mock
	DisruptorBridge outDisruptor;

	@Mock
	LoginProcessor loginProcessor;

	@Mock
	OrderProcessor orderProcessor;

	@Before
	public void setUp() {
		businessProcessor =
				new BusinessProcessor(outDisruptor)
					.withLoginProcessor(loginProcessor)
					.withOrderProcessor(orderProcessor);
	}

	void setupUserAsSytemAdmin() {
		Mockito.when(loginProcessor.sessionDetails(Mockito.any(AuthenticatedRequest.class))).thenReturn(UserSession.systemUserSession());
	}

	void setupUserAsLoggedIn() {
		Mockito.when(loginProcessor.sessionDetails(Mockito.any(AuthenticatedRequest.class))).thenReturn(UserSession.validUserSession());
	}

	@Test
	public void shouldDispatchLoginRequest() {
		Login request = new Login();
		businessProcessor.dispatchRequest(request);
		verify(loginProcessor).login(request);
	}

	@Test
	public void shouldDispatchUserRegistrationRequest() {
		setupUserAsSytemAdmin();
		UserRegistrationRequest request = new UserRegistrationRequest();
		businessProcessor.dispatchRequest(request);
		verify(loginProcessor).registerUser(request);
	}

	@Test
	public void shouldDispatchAccountStateRequest() {
		setupUserAsLoggedIn();
		AccountStateRequest request = new AccountStateRequest();
		businessProcessor.dispatchRequest(request);
		verify(loginProcessor).accountState(request);
	}
	
	@Test
	public void shouldAddCreditRequest() {
		setupUserAsSytemAdmin();
		AddCreditRequest request = new AddCreditRequest();
		businessProcessor.dispatchRequest(request);
		verify(loginProcessor).addCredit(request);
	}
	
	@Test
	public void shouldDispatchWithdrawRequest() {
		setupUserAsSytemAdmin();
		WithdrawRequest request = new WithdrawRequest();
		businessProcessor.dispatchRequest(request);
		verify(loginProcessor).withdraw(request);
	}

	@Test
	public void shouldDispatchOrderRequest() {
		setupUserAsLoggedIn();
		Order request = new Order();
		businessProcessor.dispatchRequest(request);
		verify(orderProcessor).order(request);
	}

	@Test
	public void shouldDispatchMarketRegistrationRequest() {
		setupUserAsSytemAdmin();
		MarketRegistrationRequest request = new MarketRegistrationRequest();
		businessProcessor.dispatchRequest(request);
		verify(orderProcessor).registerMarket(request);
	}

	@Test
	public void shouldDispatchMarketPricesRequest() {
		setupUserAsLoggedIn();
		MarketPricesRequest request = new MarketPricesRequest();
		businessProcessor.dispatchRequest(request);
		verify(orderProcessor).marketPrices(request);
	}
	
	@Test
	public void shouldDispatchAvailableMarketsRequest() {
		setupUserAsLoggedIn();
		MarketsRequest request = new MarketsRequest();
		businessProcessor.dispatchRequest(request);
		verify(orderProcessor).availableMarkets(request);
	}

	@Test(expected=IllegalArgumentException.class)
	public void shouldThrowIllegalArgumentExceptionOnUnsupportedRequestSubtype() {
		businessProcessor.dispatchRequest(new Request() {});
	}
}
