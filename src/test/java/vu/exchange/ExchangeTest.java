package vu.exchange;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

public class ExchangeTest {

	private static final String APP_CONFIG = "env/boikoro/app.properties";
	private static final AppContext APP_CONTEXT = new AppContext(new File(APP_CONFIG));

	@Before
	public void setUp(){
		APP_CONTEXT.appLockFile().delete();
		APP_CONTEXT.appPidFile().delete();
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
}
