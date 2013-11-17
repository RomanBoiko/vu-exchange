package vu.exchange;

import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

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

	@Test
	public void shouldDispatchLoginRequest() {
		Login request = new Login();
		businessProcessor.dispatchRequest(request);
		verify(loginProcessor).login(request);
	}
	
	@Test
	public void shouldDispatchUserRegistrationRequest() {
		UserRegistrationRequest request = new UserRegistrationRequest();
		businessProcessor.dispatchRequest(request);
		verify(loginProcessor).registerUser(request);
	}

	@Test
	public void shouldDispatchAccountStateRequest() {
		AccountStateRequest request = new AccountStateRequest();
		businessProcessor.dispatchRequest(request);
		verify(loginProcessor).accountState(request);
	}
	
	@Test
	public void shouldAddCreditRequest() {
		AddCreditRequest request = new AddCreditRequest();
		businessProcessor.dispatchRequest(request);
		verify(loginProcessor).addCredit(request);
	}
	
	@Test
	public void shouldDispatchWithdrawRequest() {
		WithdrawRequest request = new WithdrawRequest();
		businessProcessor.dispatchRequest(request);
		verify(loginProcessor).withdraw(request);
	}

	@Test
	public void shouldDispatchOrderRequest() {
		Order request = new Order();
		businessProcessor.dispatchRequest(request);
		verify(orderProcessor).order(request);
	}

	@Test
	public void shouldDispatchMarketRegistrationRequest() {
		MarketRegistrationRequest request = new MarketRegistrationRequest();
		businessProcessor.dispatchRequest(request);
		verify(orderProcessor).registerMarket(request);
	}

	@Test
	public void shouldDispatchMarketPricesRequest() {
		MarketPricesRequest request = new MarketPricesRequest();
		businessProcessor.dispatchRequest(request);
		verify(orderProcessor).marketPrices(request);
	}
	
	@Test
	public void shouldDispatchAvailableMarketsRequest() {
		MarketsRequest request = new MarketsRequest();
		businessProcessor.dispatchRequest(request);
		verify(orderProcessor).availableMarkets(request);
	}

	@Test(expected=IllegalArgumentException.class)
	public void shouldThrowIllegalArgumentExceptionOnUnsupportedRequestSubtype() {
		businessProcessor.dispatchRequest(new Request() {});
	}

}
