package vu.exchange;

import static java.lang.Integer.parseInt;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

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

	File appLockFile() {
		return new File(this.property("app.lock.file"));
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

	Integer apiTcpPort() {
		return parseInt(this.property("api.tcp.port"));
	}

	String property(String key) {
		return appProperties.getProperty(key);
	}
}