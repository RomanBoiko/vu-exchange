package vu.exchange;

import static java.lang.String.format;

import static vu.exchange.DisruptorBridge.multipleProducersSingleConsumer;
import static vu.exchange.DisruptorBridge.singleProducerMultipleConsumers;

import java.io.File;
import java.lang.management.ManagementFactory;

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
			AppContext appContext = new AppContext(config);
			initLogging(appContext);
			Logger.getLogger(Exchange.class).info(format("Run app using config: %s", config.getAbsolutePath()));
			new Exchange(appContext).init().start().keepAppRunning().stop();
		} catch (Exception e) {
			System.err.println("error in application, exiting");
			throw new RuntimeException(e);
		}
	}

	private final Logger log = Logger.getLogger(Exchange.class);
	private final AppContext appContext;
	private final DisruptorBridge requestSubmitDisruptor;
	private final DisruptorBridge responsePublishDisruptor;
	private final RequestResponseRepo requestResponseRepo = new RequestResponseRepo();
	private final TcpServer tcpServer;

	Exchange(AppContext context) throws Exception {
		this.appContext = context;
		this.responsePublishDisruptor = singleProducerMultipleConsumers(new ResponsePublisher(requestResponseRepo));
		BusinessProcessor businessProcessor = new BusinessProcessor(responsePublishDisruptor)
			.withLoginProcessor(new LoginProcessor()
				.withSystemUserName(appContext.systemUserName())
				.withSystemUserPassword(appContext.systemUserPassword()))
			.withOrderProcessor(new OrderProcessor());
		this.requestSubmitDisruptor = multipleProducersSingleConsumer(businessProcessor);
		this.tcpServer = new TcpServer()
					.withPort(appContext.apiTcpPort())
					.withNumberOfWorkers(context.inputReceiversCount())
					.withRequestHandlerFactory(
							new RequestHandlerFactory(
									requestResponseRepo,
									appContext.inputEventsFile(),
									requestSubmitDisruptor));
	}

	Exchange init() throws Exception {
		if (appContext.appLockFile().exists()) {
			throw new IllegalStateException("Lock file exists, another instance of application running?");
		} else {
			createApplicationFiles();
		}
		return this;
	}

	Exchange start() {
		responsePublishDisruptor.start();
		requestSubmitDisruptor.start();
		tcpServer.start();
		return this;
	}

	void stop() throws Exception {
		log.info("Stopping exchange");
		tcpServer.stop();
		log.info("External API server stopped");
		requestSubmitDisruptor.stop();
		log.info("Request disruptor stopped");
		responsePublishDisruptor.stop();
		log.info("Response disruptor stopped");
		appContext.appPidFile().delete();
		log.info("Exchange stopped, pid and lock files removed");
	}

	private Exchange keepAppRunning() throws Exception {
		while (appContext.appLockFile().exists()) {
			Thread.sleep(2 * 1000);
		}
		log.info("Lock file disappeared, stopping app");
		return this;
	}

	private void createApplicationFiles() throws Exception {
		appContext.appPidFile().getParentFile().mkdirs();
		Files.write(currentProcessId(), appContext.appPidFile(), Charsets.UTF_8);
		log.info("PID file written: " + appContext.appPidFile().getAbsolutePath());
		Files.write(currentProcessId(), appContext.appLockFile(), Charsets.UTF_8);
		log.info("Lock file written: " + appContext.appLockFile().getAbsolutePath());
	}

	private String currentProcessId() {
		final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
		return jvmName.substring(0, jvmName.indexOf('@'));
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
