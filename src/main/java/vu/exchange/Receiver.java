package vu.exchange;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class Receiver {
	static final String INPUT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss:SSSz";

	private final Logger log = Logger.getLogger(this.getClass());

	private final File incomingEventsFile;
	private final DateFormat dateFormat = new SimpleDateFormat(
			INPUT_DATE_FORMAT);
	private final CallableProcessor eventProcessor;

	public Receiver(File incomingEventsFile, CallableProcessor eventProcessor) {
		this.incomingEventsFile = incomingEventsFile;
		this.eventProcessor = eventProcessor;
	}

	void onMessage(String message) {
		Date arrivalTime = new Date(System.currentTimeMillis());
		log.debug("Order receiveed: " + message);
		Order order = orderFromString(message);
		order.withArrivalTimestamp(arrivalTime);
		persistIncomingEvent(arrivalTime, message);
		log.debug("Order persisted");
		eventProcessor.process(order);
		log.debug("Order forwarded for further processing");
	}

	private void persistIncomingEvent(Date arrivalTime, String message) {
		try {
			Files.append(format("%s=%s\n", dateFormat.format(arrivalTime), message), incomingEventsFile, Charsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	static Order orderFromString(String json) {
		try {
			JsonFactory f = new JsonFactory();
			JsonParser parser = f.createJsonParser(json);
			ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(parser, Order.class);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}

class Server implements Runnable {// change using http://zguide.zeromq.org/java:mtserver
	final Receiver receiver;
	private volatile AtomicBoolean toStop = new AtomicBoolean(false);
	private ZMQ.Socket socket;
	private ZMQ.Context context;

	Server(Receiver handler, ZMQ.Context context) {
		this.receiver = handler;
		this.context = context;
	}

	void stop() {
		toStop.set(true);
		socket.close();
	}

	@Override
	public void run() {
		socket = context.socket(ZMQ.REP);
		socket.bind("tcp://127.0.0.1:5555");

		while (!toStop.get()) {
			String msg = null;
			try {
				msg = new String(socket.recv(0));
			} catch (ZMQException.IOException e) {
				break;
			}
			receiver.onMessage(msg);

			socket.send("{\"received\" : \"ok\"}", 0);
		}
	}
}
