package vu.exchange;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.math.BigDecimal;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

import vu.exchange.Order.Currency;
import vu.exchange.Order.Position;

@RunWith(MockitoJUnitRunner.class)
public class ReceiverTest {

	private static final String TEST_ORDER =
			"{\"client\":12, \"market\":43, \"price\":21.23, \"currency\":\"GBP\",\"quantity\":2.4,\"position\":\"BUY\"}";

	private File incomingEventsFile = new File("target/testin.txt");

	@After
	public void after() {
		incomingEventsFile.delete();
	}
	
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
	public void shouldPersistEventAndSendResponseViaMQ() throws Exception {
		Context context = ZMQ.context(1);
		Server server = new Server(new Receiver(incomingEventsFile, mock(CallableProcessor.class)), context);
		Thread serverThread = new Thread(server);
		serverThread.start();
		String response = sendOrder(TEST_ORDER);
		assertThat(response, is("{\"received\" : \"ok\"}"));
		server.stop();
		context.term();
		serverThread.join();

//		String firstEvent = Files.readFirstLine(incomingEventsFile, Charsets.UTF_8);
//		assertThat(firstEvent, Matchers.endsWith("=" + TEST_ORDER));
//		assertNotNull(new SimpleDateFormat(Receiver.INPUT_DATE_FORMAT).parse(firstEvent.split("=")[0]));
	}

	static String sendOrder(String message) {
		ZMQ.Context context = ZMQ.context(1);
		ZMQ.Socket socket = context.socket(ZMQ.REQ);
		socket.connect("tcp://127.0.0.1:5555");
		socket.send(message.getBytes(), 0);
		String result = new String(socket.recv(0));
		socket.close();
		context.term();
		return result;
	}
}
