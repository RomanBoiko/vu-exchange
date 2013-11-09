package vu.exchange;

import static java.lang.String.format;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.channels.ClosedChannelException;

import org.apache.log4j.Logger;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.IConnectionScoped;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;

import vu.exchange.RequestHandler.RequestHandlerFactory;

public class TcpServer {

	private final Logger log = Logger.getLogger(this.getClass());
	private RequestHandlerFactory requestHandlerFactory;
	private int port = 5555;
	private int workersNumber = 1;
	private IServer ioServer;

	public TcpServer withPort(int port) {
		this.port = port;
		return this;
	}

	public TcpServer withRequestHandlerFactory(RequestHandlerFactory requestHandlerFactory) {
		this.requestHandlerFactory = requestHandlerFactory;
		return this;
	}

	public TcpServer withNumberOfWorkers(int workersNumber) {
		this.workersNumber = workersNumber;
		return this;
	}

	public TcpServer start() {
		log.info("Starting ApiServer");
		try {
			ioServer = new Server(port, new ApiRequestHandler(requestHandlerFactory), 1, workersNumber);
			ioServer.start();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		log.info("ApiServer started");
		return this;
	}

	void stop() throws Exception {
		log.info("Stopping ApiServer");
		ioServer.close();
		log.info("ApiServer stopped");
	}

	class ApiRequestHandler implements IDataHandler, IConnectionScoped {
		private static final String DELIMITER = "\n";
		private final Logger log = Logger.getLogger(this.getClass());
		private final RequestHandlerFactory requestHandlerFactory;
		private final RequestHandler requestHandler;

		public ApiRequestHandler(RequestHandlerFactory requestHandlerFactory) {
			this.requestHandlerFactory = requestHandlerFactory;
			this.requestHandler = requestHandlerFactory.createHandler();
		}
		
		private String getResponse(String request) {
			try {
				return requestHandler.getResponseMessage(request);
			} catch (Exception e) {
				log.error(e);
				return String.format("{\"error\" : \"%s\", \"onRequest\" : %s}", e.getMessage(), request);
			}
		}

		@Override
		public boolean onData(INonBlockingConnection connection)
				throws IOException, BufferUnderflowException,
				ClosedChannelException, MaxReadSizeExceededException {
			String request = connection.readStringByDelimiter(DELIMITER);
			log.debug(format("Request received by worker: %s", request));
			String responseMsg = getResponse(request);
			log.debug(format("Reply sent by worker: %s", responseMsg));
			connection.write(responseMsg + DELIMITER);
			return true;
		}

		public Object clone() throws java.lang.CloneNotSupportedException {
			return new ApiRequestHandler(requestHandlerFactory);
		}
	}
}
