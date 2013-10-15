package vu.exchange;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static vu.exchange.ExchangeDisruptor.multipleProducersSingleConsumer;
import static vu.exchange.ExchangeDisruptor.singleProducerMultipleConsumers;

import java.io.File;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.zeromq.ZMQ;

import vu.exchange.RequestHandler.RequestHandlerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.lmax.disruptor.EventHandler;

public class ExchangeTest {

	private static final String TEST_ENV_CONFIG = "env/boikoro/app.properties";

//	@Test
//	public void shouldStartAndStopServer() throws Exception {
////		Mockito.when(requestHandler.onMessage(testRequest)).thenwhen
//		RequestResponseRepo requestResponseRepo = new RequestResponseRepo();
//		EventHandler<ValueEvent> responsePublisher = new ResponsePublisher(requestResponseRepo);
//		RequestHandlerFactory requestHandlerFactory = new RequestHandlerFactory(
//				requestResponseRepo,
//				new File("target/eventsRepo"),
//				multipleProducersSingleConsumer(
//						new BusinessProcessor(
//								singleProducerMultipleConsumers(responsePublisher))));
//		ApiServer server = new ApiServer()
//			.withApiPort(5555)
//			.withNumberOfWorkers(1)
//			.withRequestHandlerFactory(requestHandlerFactory).start();
////		String response = sendOrder(TestMessageRepo.BUY_ORDER);
////		assertThat(response, is("{\"received\" : \"ok\"}"));
//		server.stop();
//	}

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

//	@Test
//	public void shouldCreatePidFileDuringStartAndStopAfterItWasRemoved() throws Exception {
//		AppContext context = new AppContext(new File(TEST_ENV_CONFIG));
//		if(context.appPidFile().exists() ) {
//			context.appPidFile().delete();
//		}
//		assertThat(context.appPidFile().exists(), is(false));
//		Thread appThread = new Thread(new Runnable() {
//			@Override
//			public void run() {
//				Exchange.main(new String[]{TEST_ENV_CONFIG});
//			}
//		});
//		appThread.start();
//		Thread.sleep(1000);
//		assertThat(context.appPidFile().exists(), is(true));
//		assertThat(Integer.parseInt(Files.readFirstLine(context.appPidFile(), Charsets.UTF_8)), Matchers.greaterThan(0));
//		assertTrue(context.appPidFile().delete());
//		appThread.join();
//	}

}
