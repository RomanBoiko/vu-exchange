package vu.exchange;

import static vu.exchange.ExchangeDisruptor.multipleProducersSingleConsumer;
import static vu.exchange.ExchangeDisruptor.singleProducerMultipleConsumers;

import java.io.File;
import java.io.FileInputStream;
import java.lang.management.ManagementFactory;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.zeromq.ZMQ;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class Exchange {
	public static void main(String[] args) {
		try {
			File config = new File(args[0]);
			assert config.exists() && config.isFile();
			AppContext appContext = new AppContext(config);
			initLogging(appContext);
			Logger log = Logger.getLogger(Exchange.class);
			if (appContext.appPidFile().exists()) {
				throw new IllegalStateException(
						"Pid file exists, another instance of application could be already running");
			} else {
				appContext.appPidFile().getParentFile().mkdirs();
				Files.write(currentProcessId(), appContext.appPidFile(),
						Charsets.UTF_8);
			}
			Exchange exchange = new Exchange(appContext).start();
			log.info(String.format("app started using config: %s",
					config.getAbsolutePath()));
			while (appContext.appPidFile().exists()) {
				Thread.sleep(2 * 1000);
			}
			exchange.stop();
			appContext.appPidFile().delete();
			log.info("app stopped, pid file removed");
		} catch (Exception e) {
			System.err.println("error in application, exiting");
			throw new RuntimeException(e);
		}
	}

	private static String currentProcessId() {
		final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
		return jvmName.substring(0, jvmName.indexOf('@'));
	}

	private final AppContext appContext;
	private final ZMQ.Context zmqContext;
	private final ExecutorService inputMessageReceiversPool;
	private final ExchangeDisruptor businessProcessorDisruptor;
	private final ExchangeDisruptor eventPublisherDisruptor;

	Exchange(AppContext context) {
		this.appContext = context;
		this.zmqContext = ZMQ.context(1);
		Publisher eventsPublisher = new Publisher();
		this.eventPublisherDisruptor = singleProducerMultipleConsumers(eventsPublisher);
		BusinessProcessor businessProcessor = new BusinessProcessor(
				eventPublisherDisruptor);
		this.businessProcessorDisruptor = multipleProducersSingleConsumer(businessProcessor);
		this.inputMessageReceiversPool = Executors.newFixedThreadPool(context
				.inputReceiversCount());
	}

	Exchange start() {
		eventPublisherDisruptor.start();
		businessProcessorDisruptor.start();
		for (int i = 0; i < appContext.inputReceiversCount(); i++) {
			inputMessageReceiversPool
					.submit(new Server(new Receiver(appContext
							.inputEventsFile(), businessProcessorDisruptor),
							zmqContext));
		}
		return this;
	}

	void stop() {
		inputMessageReceiversPool.shutdown();
	}

	private static void initLogging(AppContext appContext) {
		ConsoleAppender console = new ConsoleAppender();
		console.setLayout(new PatternLayout(appContext.appLogFormat()));
		console.setThreshold(Level.FATAL);
		console.activateOptions();

		FileAppender fa = new DailyRollingFileAppender();
		fa.setName("FileLogger");
		fa.setFile(appContext.appLogFilePath());
		fa.setLayout(new PatternLayout(appContext.appLogFormat()));
		fa.setThreshold(Level.DEBUG);
		fa.setAppend(true);
		fa.activateOptions();

		Logger.getRootLogger().addAppender(console);
		Logger.getRootLogger().addAppender(fa);
	}
}

class AppContext {
	private final Properties appProperties;

	AppContext(File config) {
		try {
			appProperties = new Properties();
			appProperties.load(new FileInputStream(config));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	File inputEventsFile() {
		return new File(this.property("input.events.journal"));
	}

	File appPidFile() {
		return new File(this.property("app.pid.file"));
	}

	String appLogFilePath() {
		return this.property("app.log.file");
	}

	String appLogFormat() {
		return this.property("app.log.format");
	}
	

	Integer inputReceiversCount() {
		return Integer.parseInt(this.property("input.receivers.count"));
	}

	String property(String key) {
		return appProperties.getProperty(key);
	}
}
