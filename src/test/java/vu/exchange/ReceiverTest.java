package vu.exchange;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static vu.exchange.Order.*;
import static vu.exchange.Receiver.sendOrder;

import java.math.BigDecimal;

import org.junit.Test;

import vu.exchange.Receiver.RequestHandler;
import vu.exchange.Receiver.Server;

public class ReceiverTest {

	private static final String TEST_ORDER =
			"{\"client\":12, \"market\":43, \"price\":21.23, \"currency\":\"GBP\",\"quantity\":2.4,\"position\":\"BUY\"}";

	@Test
	public void shouldDeserializeOrder() {
		Order order = Receiver.orderFromString(TEST_ORDER);
		assertThat(order.client, is(12L));
		assertThat(order.market, is(43L));
		assertThat(order.price, is(BigDecimal.valueOf(21.23)));
		assertThat(order.currency, is(Currency.GBP));
		assertThat(order.quantity, is(BigDecimal.valueOf(2.4)));
		assertThat(order.position, is(Position.BUY));
	}

	@Test
	public void shouldReceiveServerResponseViaMQ() throws Exception {
		Server server = new Server(new RequestHandler());
		Thread serverThread = new Thread(server);
		serverThread.start();
		String response = sendOrder(TEST_ORDER);
		assertThat(response, is("{\"received\" : \"ok\"}"));
		server.toStop.set(true);
		serverThread.join();
	}
}
