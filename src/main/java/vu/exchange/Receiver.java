package vu.exchange;

import java.util.concurrent.atomic.AtomicBoolean;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.jeromq.ZMQ;

public class Receiver {
	static class RequestHandler {
		void onMessage(String message) {
			System.out.println("Order receiveed: " + message);
			Order order = orderFromString(message);
			System.out.println("Order parser: " + order);
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

	static class Server implements Runnable {
		final RequestHandler handler;
		volatile AtomicBoolean toStop = new AtomicBoolean(false);

		Server(RequestHandler handler) {
			this.handler = handler;
		}

		@Override
		public void run() {
			ZMQ.Context context = ZMQ.context(1);
			ZMQ.Socket socket = context.socket(ZMQ.REP);
			socket.bind("tcp://127.0.0.1:5555");

			while (!toStop.get()) {
				byte[] msg = socket.recv(0);
				handler.onMessage(new String(msg));

				socket.send("{\"received\" : \"ok\"}", 0);
			}
			socket.close();
			context.term();
		}
	}

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
}
