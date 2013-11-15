package vu.exchange;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;

import vu.exchange.MarketRegistrationResult.MarketRegistrationStatus;
import vu.exchange.Order.Position;

public class OrderProcessorTest {

	OrderProcessor orderProcessor;
	
	@Before
	public void setUp(){
		orderProcessor = new OrderProcessor();
	}

	@Test
	public void shouldReturnNoRegisteredMarkets() throws Exception {
		Markets noMarketsYet = orderProcessor.availableMarkets(new MarketsRequest());
		assertThat(noMarketsYet.markets.size(), is(0));
	}

	@Test
	public void shouldRegisterMarket() {
		MarketRegistrationRequest brandNewMarket = new MarketRegistrationRequest().withId(11L).withName("markt");
		MarketRegistrationResult marketRegistrationResult = orderProcessor.registerMarket(brandNewMarket);
		assertThat(marketRegistrationResult.id, is(brandNewMarket.id));
		assertThat(marketRegistrationResult.registrationStatus, is(MarketRegistrationStatus.REGISTERED));
	}
	
	@Test
	public void shouldUpdateMarket() {
		MarketRegistrationRequest brandNewMarket = new MarketRegistrationRequest().withId(11L).withName("markt");
		orderProcessor.registerMarket(brandNewMarket);
		MarketRegistrationResult marketUpdateResult = orderProcessor.registerMarket(new MarketRegistrationRequest().withId(11L).withName("markt2"));
		assertThat(marketUpdateResult.id, is(brandNewMarket.id));
		assertThat(marketUpdateResult.registrationStatus, is(MarketRegistrationStatus.UPDATED));
	}

	@Test
	public void shouldReturnRegisteredMarkets() throws Exception {
		orderProcessor.registerMarket(new MarketRegistrationRequest().withId(11L).withName("markt1"));
		orderProcessor.registerMarket(new MarketRegistrationRequest().withId(22L).withName("markt2"));
		Markets markets = orderProcessor.availableMarkets(new MarketsRequest());
		assertThat(markets.markets.size(), is(2));
		assertThat(markets.markets.get(0).id, is(22L));
		assertThat(markets.markets.get(0).name, is("markt2"));
		assertThat(markets.markets.get(1).id, is(11L));
		assertThat(markets.markets.get(1).name, is("markt1"));
	}

	@Test
	public void shouldReturnNoPricesForNewMarket() throws Exception {
		orderProcessor.registerMarket(new MarketRegistrationRequest().withId(11L).withName("markt1"));
		MarketPrices marketPrices = orderProcessor.marketPrices(new MarketPricesRequest().withMarketId(11L));
		assertThat(marketPrices.marketId, is(11L));
		assertThat(marketPrices.bids.size(), is(0));
		assertThat(marketPrices.offers.size(), is(0));
	}

	@Test
	public void shouldReturnNoPricesForUnexistingMarket() throws Exception {
		MarketPrices marketPrices = orderProcessor.marketPrices(new MarketPricesRequest().withMarketId(11L));
		assertThat(marketPrices.marketId, is(11L));
		assertThat(marketPrices.bids.size(), is(0));
		assertThat(marketPrices.offers.size(), is(0));
	}

	@Test
	public void shouldReturnPricesForMarket() throws Exception {
		orderProcessor.registerMarket(new MarketRegistrationRequest().withId(11L).withName("markt1"));
		orderProcessor.order(new Order().withMarket(11L).withPosition(Position.BUY).withPrice(11.2).withQuantity(3.3));
		orderProcessor.order(new Order().withMarket(11L).withPosition(Position.BUY).withPrice(12.2).withQuantity(3.4));
		orderProcessor.order(new Order().withMarket(11L).withPosition(Position.SELL).withPrice(12.1).withQuantity(3.2));
		MarketPrices marketPrices = orderProcessor.marketPrices(new MarketPricesRequest().withMarketId(11L));
		assertThat(marketPrices.marketId, is(11L));
		assertThat(marketPrices.bids.size(), is(2));
		assertThat(marketPrices.offers.size(), is(1));
		assertThat(marketPrices.bids.get(0).price, is(BigDecimal.valueOf(12.2)));
		assertThat(marketPrices.bids.get(1).price, is(BigDecimal.valueOf(11.2)));
		assertThat(marketPrices.offers.get(0).price, is(BigDecimal.valueOf(12.1)));
	}

}
