package vu.exchange;

import static vu.exchange.ExchangeDisruptor.multipleProducersSingleConsumer;
import static vu.exchange.ExchangeDisruptor.singleProducerMultipleConsumers;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.zeromq.ZMQ;

import vu.exchange.RequestHandler.RequestHandlerFactory;

import com.lmax.disruptor.EventHandler;

@RunWith(MockitoJUnitRunner.class)
public class ApiServerTest {

	@Mock RequestHandler requestHandler;
	String testRequest = "req";

	@Test
	public void shouldStartAndStopServer() throws Exception {
//		Mockito.when(requestHandler.onMessage(testRequest)).thenwhen
		RequestResponseRepo requestResponseRepo = new RequestResponseRepo();
		EventHandler<ValueEvent> responsePublisher = new ResponsePublisher(requestResponseRepo);
		RequestHandlerFactory requestHandlerFactory = new RequestHandlerFactory(
				requestResponseRepo,
				new File("target/eventsRepo"),
				multipleProducersSingleConsumer(
						new BusinessProcessor(
								singleProducerMultipleConsumers(responsePublisher))));
		ApiServer server = new ApiServer()
			.withApiPort(5555)
			.withNumberOfWorkers(1)
			.withRequestHandlerFactory(requestHandlerFactory).start();
//		String response = sendOrder(TestMessageRepo.BUY_ORDER);
//		assertThat(response, is("{\"received\" : \"ok\"}"));
		server.stop();
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
