package vu.exchange;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;

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
		Response apiResponse = getE2EExchangeResponse(new Order());
		assertThat(apiResponse, instanceOf(OrderSubmitResult.class));
		assertThat(((OrderSubmitResult)apiResponse).status, is(OrderSubmitResult.OrderStatus.ACCEPTED));
	}

	@Test
	public void shouldLoginUser() throws Exception {
		Response apiResponse = getE2EExchangeResponse(new Login().withEmail("user1@smarkets.com").withPassword("pass1"));
		assertThat(apiResponse, instanceOf(LoginResult.class));
		assertThat(((LoginResult)apiResponse).status, is(LoginResult.LoginStatus.OK));
		assertThat(((LoginResult)apiResponse).sessionId, Matchers.notNullValue());
	}

	private Response getE2EExchangeResponse(Request request)
			throws Exception {
		Exchange exchange = new Exchange(APP_CONTEXT);
		exchange.start();
		String response = sendMessage(request.toJson());
		exchange.stop();
		Response apiResponse = Response.fromJson(response);
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

	private String sendMessage(String request) throws IOException {
		IBlockingConnection bc = new BlockingConnection("localhost", APP_CONTEXT.apiTcpPort());
		bc.write(request + "\n");
		String response = bc.readStringByDelimiter("\n");
		System.out.println(String.format("%s %s {%s}", "CLIENT", "RCV", response));
		bc.close();
		return response;
	}

}
