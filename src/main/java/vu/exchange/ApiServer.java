package vu.exchange;

import static java.lang.String.format;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.channels.ClosedChannelException;

import org.apache.log4j.Logger;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.IConnectHandler;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;

import vu.exchange.RequestHandler.RequestHandlerFactory;

public class ApiServer {

	private final Logger log = Logger.getLogger(this.getClass());
	private RequestHandlerFactory requestHandlerFactory;
	private int port = 5555;
	private int workersNumber = 1;
	private IServer ioServer;

	public ApiServer withApiPort(int port) {
		this.port = port;
		return this;
	}

	public ApiServer withRequestHandlerFactory(
			RequestHandlerFactory requestHandlerFactory) {
		this.requestHandlerFactory = requestHandlerFactory;
		return this;
	}

	public ApiServer withNumberOfWorkers(int workersNumber) {
		this.workersNumber = workersNumber;
		return this;
	}

	public ApiServer start() {
		log.info("Starting ApiServer");
		try {
			ioServer = new Server(port, new ApiRequestHandler(requestHandlerFactory.createHandler()), 1, workersNumber);
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

	class ApiRequestHandler implements IConnectHandler, IDataHandler {
		private static final String DELIMITER = "\n";
		private final Logger log = Logger.getLogger(this.getClass());
		private final RequestHandler requestHandler;

		public ApiRequestHandler(RequestHandler requestHandler) {
			this.requestHandler = requestHandler;
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

		@Override
		public boolean onConnect(INonBlockingConnection connection)
				throws IOException, BufferUnderflowException,
				MaxReadSizeExceededException {
			return true;
		}

	}
}
