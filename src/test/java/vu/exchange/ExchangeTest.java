package vu.exchange;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.hamcrest.Matchers;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class ExchangeTest {

	private static final String TEST_ENV_CONFIG = "env/boikoro/app.properties";

	@Test
	public void shouldCreatepidFileDuringStartAndStopAfterItWasRemoved() throws Exception {
		AppContext context = new AppContext(new File(TEST_ENV_CONFIG));
		assertThat(context.appPidFile().exists(), is(false));
		Thread appThread = new Thread(new Runnable() {
			@Override
			public void run() {
				Exchange.main(new String[]{TEST_ENV_CONFIG});
			}
		});
		appThread.start();
		Thread.sleep(1000);
		assertThat(context.appPidFile().exists(), is(true));
		assertThat(Integer.parseInt(Files.readFirstLine(context.appPidFile(), Charsets.UTF_8)), Matchers.greaterThan(0));
		assertTrue(context.appPidFile().delete());
		appThread.join();
	}

}
