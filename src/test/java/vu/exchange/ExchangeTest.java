package vu.exchange;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.PollItem;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;


public class ExchangeTest {

	private static final String APP_CONFIG = "env/boikoro/app.properties";
	private static final AppContext APP_CONTEXT = new AppContext(new File(APP_CONFIG));

	@Before
	public void setUp(){
		APP_CONTEXT.appLockFile().delete();
		APP_CONTEXT.appPidFile().delete();
	}

	@Test
	public void shouldAcceptOrders() throws Exception {
		ApiResponse apiResponse = getE2EExchangeResponse(new Order());
		assertThat(apiResponse, instanceOf(OrderSubmitResult.class));
		assertThat(((OrderSubmitResult)apiResponse).status, is(OrderSubmitResult.OrderStatus.ACCEPTED));
	}

//	@Test
//	public void shouldLoginUser() throws Exception {
//		ApiResponse apiResponse = getE2EExchangeResponse(new Login());
//		assertThat(apiResponse, instanceOf(LoginResult.class));
//		assertThat(((LoginResult)apiResponse).status, is(LoginResult.LoginStatus.OK));
//		assertThat(((LoginResult)apiResponse).sessionId, notNullValue());
//	}

	private ApiResponse getE2EExchangeResponse(ApiRequest request)
			throws Exception {
		Exchange exchange = new Exchange(APP_CONTEXT);
		exchange.start();
		String response = sendMessage(request.toJson());
		exchange.stop();
		ApiResponse apiResponse = ApiResponse.fromJson(response);
		return apiResponse;
	}
	
	@Test
	public void shouldCreatePidFileOnStartAndRemoveOnStop() throws Exception {
		Thread appThread = new Thread(new Runnable(){
			public void run() {
				Exchange.main(new String[]{APP_CONFIG});
			}
		});
		assertThat(APP_CONTEXT.appPidFile().exists(), is(false));
		assertThat(APP_CONTEXT.appLockFile().exists(), is(false));
		appThread.start();
		Thread.sleep(1000);
		assertThat(APP_CONTEXT.appPidFile().exists(), is(true));
		assertThat(APP_CONTEXT.appLockFile().exists(), is(true));
		APP_CONTEXT.appLockFile().delete();
		appThread.join();
		assertThat(APP_CONTEXT.appPidFile().exists(), is(false));
	}

	private String sendMessage(String message) {
		ZContext ctx = new ZContext();
		Socket client = ctx.createSocket(ZMQ.DEALER);

		String clientId = String.format("c111");
		client.setIdentity(clientId.getBytes());
		client.connect(String.format("tcp://localhost:%s", APP_CONTEXT.apiTcpPort()));
		client.send(message, 0);

		PollItem[] items = new PollItem[] { new PollItem(client, Poller.POLLIN) };
		String response = null;
		while (true) {
			ZMQ.poll(items, 10);
			if (items[0].isReadable()) {
				ZMsg msg = ZMsg.recvMsg(client);
				logAction("CLIENT", "RCV", clientId, new String(msg.getLast().getData()));
				response = new String(msg.getLast().getData());
				msg.destroy();
				ctx.destroy();
				ctx.close();
				return response;
			}
		}
	}

	static void logAction(String user, String action, String userId, String message) {
		System.out.println(String.format("%s %s %s {%s}", user, userId, action, message));
	}

}
