package vu.exchange;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static vu.exchange.Order.*;

import java.math.BigDecimal;

import org.junit.Test;

public class ReceiverTest {

	@Test
	public void shouldDeserializeOrder() {
		Order order = Receiver.orderFromString(
			"{\"client\":12, \"market\":43, \"price\":21.23, \"currency\":\"GBP\",\"quantity\":2.4,\"position\":\"BUY\"}");
		assertThat(order.client, is(12L));
		assertThat(order.market, is(43L));
		assertThat(order.price, is(BigDecimal.valueOf(21.23)));
		assertThat(order.currency, is(Currency.GBP));
		assertThat(order.quantity, is(BigDecimal.valueOf(2.4)));
		assertThat(order.position, is(Position.BUY));
	}

}
