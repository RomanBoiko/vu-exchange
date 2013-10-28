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

public class ApiRequestTest {

	private static final String TEST_ORDER = "{\"type\":\"Order\",\"session\":\"sessionId\",\"market\":1,\"price\":1.1,\"currency\":\"GBP\",\"quantity\":1.2,\"position\":\"SELL\"}";

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
		Order order = new Order().withMarket(1L).withPosition(Position.SELL).withPrice(1.1).withQuantity(1.2).withSession("sessionId");
		String serializedOrder = order.toJson();
		assertThat(serializedOrder, is(TEST_ORDER));
	}

	@Test
	public void shouldDeserializeOrder() throws Exception {
		Order order = (Order)ApiRequest.fromJson(TEST_ORDER);
		assertThat(order.market, is(1L));
		assertThat(order.price, is(BigDecimal.valueOf(1.1)));
		assertThat(order.currency, is(Currency.GBP));
		assertThat(order.quantity, is(BigDecimal.valueOf(1.2)));
		assertThat(order.position, is(Position.SELL));
	}

	private String idPrefix(Long id) {
		return id.toString().substring(0, (id.toString().length() - Order.ID_SUFFIX_LENGTH));
	}
}
