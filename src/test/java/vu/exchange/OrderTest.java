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

public class OrderTest {

	@Test
	public void shouldGenerateOrderWithTimestampAsPartOfUniqueId() {
		Order order = new Order();
		order.withArrivalTimestamp(new Date());
		Long orderTimestamp = order.timestamp();
		Long orderId = order.id;
		
		assertThat(orderTimestamp.toString().length(), is(Order.ID_SUFFIX_LENGTH));
		assertThat(orderId.toString().length(), Matchers.greaterThan(Order.ID_SUFFIX_LENGTH));
		assertThat(orderId.toString().length(), Matchers.lessThanOrEqualTo(Order.ID_MAX_PREFIX_LENGTH + Order.ID_SUFFIX_LENGTH));
		assertThat(orderId.toString(), endsWith(orderTimestamp.toString()));
		assertThat(idPrefix(orderId), not(idPrefix(new Order().withArrivalTimestamp(new Date()).id)));
	}

	@Test
	public void shouldDeserializeOrder() throws Exception {
		Order order = Order.fromJson(TestMessageRepo.BUY_ORDER);
		assertThat(order.client, is(12L));
		assertThat(order.market, is(43L));
		assertThat(order.price, is(BigDecimal.valueOf(21.23)));
		assertThat(order.currency, is(Currency.GBP));
		assertThat(order.quantity, is(BigDecimal.valueOf(2.4)));
		assertThat(order.position, is(Position.BUY));
	}

	private String idPrefix(Long id) {
		return id.toString().substring(0, (id.toString().length() - Order.ID_SUFFIX_LENGTH));
	}
}
