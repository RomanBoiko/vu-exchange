package vu.exchange;

import static vu.exchange.ExchangeDisruptor.multipleProducersSingleConsumer;
import static vu.exchange.ExchangeDisruptor.singleProducerMultipleConsumers;

import java.io.File;
import java.io.FileInputStream;
import java.lang.management.ManagementFactory;
import java.util.Properties;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import vu.exchange.RequestHandler.RequestHandlerFactory;

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
				Files.write(currentProcessId(), appContext.appPidFile(), Charsets.UTF_8);
			}
			Exchange exchange = new Exchange(appContext).start();
			log.info(String.format("app started using config: %s", config.getAbsolutePath()));
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
	private final ExchangeDisruptor requestSubmitDisruptor;
	private final ExchangeDisruptor responsePublishDisruptor;
	private final RequestResponseRepo requestResponseRepo = new RequestResponseRepo();
	private final ApiServer apiServer;

	Exchange(AppContext context) {
		this.appContext = context;
		ResponsePublisher responsesPublisher = new ResponsePublisher(requestResponseRepo);
		this.responsePublishDisruptor = singleProducerMultipleConsumers(responsesPublisher);
		BusinessProcessor businessProcessor = new BusinessProcessor(responsePublishDisruptor);
		this.requestSubmitDisruptor = multipleProducersSingleConsumer(businessProcessor);
		RequestHandlerFactory requestHandlerFactory = new RequestHandlerFactory(
				requestResponseRepo,
				appContext.inputEventsFile(),
				requestSubmitDisruptor);
		this.apiServer = new ApiServer()
			.withApiPort(5555)
			.withNumberOfWorkers(context.inputReceiversCount())
			.withRequestHandlerFactory(requestHandlerFactory);
	}

	Exchange start() {
		responsePublishDisruptor.start();
		requestSubmitDisruptor.start();
		apiServer.start();
		return this;
	}

	void stop() throws Exception {
		apiServer.stop();
		requestSubmitDisruptor.stop();
		responsePublishDisruptor.stop();
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
