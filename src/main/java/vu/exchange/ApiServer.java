package vu.exchange;


import static java.lang.String.format;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

import vu.exchange.RequestHandler.RequestHandlerFactory;

public class ApiServer implements Runnable {
	private static final String API_URL_TEMPLATE = "tcp://*:%s";
	private static final String WORKERS_URL = "inproc://backend";

	private RequestHandlerFactory requestHandlerFactory;
	private ZContext context;
	private int port = 5555;
	private int workersNumber = 1;
	private Thread serverThread = new Thread(this);
	private ExecutorService workersExecutorService;


	public ApiServer withApiPort(int port) {
		this.port = port;
		return this;
	}

	public ApiServer withRequestHandlerFactory(RequestHandlerFactory requestHandlerFactory) {
		this.requestHandlerFactory = requestHandlerFactory;
		return this;
	}

	public ApiServer withNumberOfWorkers(int workersNumber) {
		this.workersNumber = workersNumber;
		return this;
	}

	public ApiServer start() {
		serverThread.start();
		return this;
	}

	void stop() throws Exception {
		workersExecutorService.shutdown();
		serverThread.join();
	}
	
	public void run() {
		context = new ZContext();

		Socket frontend = context.createSocket(ZMQ.ROUTER);
		frontend.bind(format(API_URL_TEMPLATE, port));

		Socket backend = context.createSocket(ZMQ.DEALER);
		backend.bind(WORKERS_URL);
		
		workersExecutorService = Executors.newFixedThreadPool(workersNumber);
		for (int threadNbr = 0; threadNbr < workersNumber; threadNbr++) {
			workersExecutorService.submit(new Worker(context, requestHandlerFactory.createHandler()));
		}
		ZMQ.proxy(frontend, backend, null);
		context.destroy();
	}

	static class Worker implements Runnable {

		private static int workerSequence = 0;

		private final ZContext ctx;
		private final Logger log = Logger.getLogger(this.getClass());
		private final RequestHandler requestHandler;

		public Worker(ZContext ctx, RequestHandler requestHandler) {
			this.ctx = ctx;
			this.requestHandler = requestHandler;
		}
	
		private String getResponse(String request) throws Exception {
			return requestHandler.getResponseMessage(request);
		}

		public void run() {
			Socket worker = ctx.createSocket(ZMQ.DEALER);
			worker.connect(WORKERS_URL);
			String workerId = "w" + ++workerSequence;
			while (!Thread.currentThread().isInterrupted()) {
				ZMsg msg = ZMsg.recvMsg(worker);
				ZFrame address = msg.pop();
				ZFrame content = msg.pop();
				assert (content != null);
				String request = new String(content.getData());
				log.debug(format("Request received by worker %s: %s", workerId, request));
				msg.destroy();
				address.send(worker, ZFrame.REUSE + ZFrame.MORE);
				String responseMsg;
				try {
					responseMsg = getResponse(request);
				} catch (Exception e) {
					log.error(e);
					responseMsg = String.format("{\"error\" : \"%s\", \"onRequest\" : %s}", e.getMessage(), request);
				}
				log.debug(format("Reply sent by worker %s: %s", workerId, responseMsg));
				ZFrame response = new ZFrame(responseMsg);
				response.send(worker, ZFrame.REUSE);
				response.destroy();
				address.destroy();
				content.destroy();
			}
			ctx.destroy();
		}
	}
}
