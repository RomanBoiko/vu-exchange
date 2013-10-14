package vu.exchange;

import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.PollItem;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;
import org.zeromq.ZMQ.Poller;

import java.util.Random;

public class ZmqTest {
	private enum User {CLIENT, WORKER}
	private enum Action {SND, RCV}
	private static Random rand = new Random(System.nanoTime());

	private static class client_task implements Runnable {
		private static int clientSequence = 0;
		public void run() {
			@SuppressWarnings("resource")
			ZContext ctx = new ZContext();
			Socket client = ctx.createSocket(ZMQ.DEALER);

			String clientId = String.format("c"+ ++clientSequence);
			client.setIdentity(clientId.getBytes());
			client.connect("tcp://localhost:5570");

			PollItem[] items = new PollItem[] { new PollItem(client,
					Poller.POLLIN) };

			int requestNbr = 0;
			while (!Thread.currentThread().isInterrupted()) {
				// Tick once per second, pulling in arriving messages
				for (int centitick = 0; centitick < 100; centitick++) {
					ZMQ.poll(items, 10);
					if (items[0].isReadable()) {
						ZMsg msg = ZMsg.recvMsg(client);
//						msg.getLast().print(identity);
						logAction(User.CLIENT, Action.RCV, clientId, new String(msg.getLast().getData()));
						msg.destroy();
					}
				}
				String message = String.format("request #%d from %s", ++requestNbr, clientId);
				client.send(message, 0);
				logAction(User.CLIENT, Action.SND, clientId, message);
			}
			ctx.destroy();
		}
	}

	private static class server_task implements Runnable {
		public void run() {
			ZContext ctx = new ZContext();

			Socket frontend = ctx.createSocket(ZMQ.ROUTER);
			frontend.bind("tcp://*:5570");

			Socket backend = ctx.createSocket(ZMQ.DEALER);
			backend.bind("inproc://backend");

			for (int threadNbr = 0; threadNbr < 5; threadNbr++)
				new Thread(new Worker(ctx)).start();

			ZMQ.proxy(frontend, backend, null);

			ctx.destroy();
		}
	}

	private static class Worker implements Runnable {
		private static int workerSequence = 0;
		private ZContext ctx;

		public Worker(ZContext ctx) {
			this.ctx = ctx;
		}

		public void run() {
			Socket worker = ctx.createSocket(ZMQ.DEALER);
			worker.connect("inproc://backend");
			String workerId = "w" + ++workerSequence;
			while (!Thread.currentThread().isInterrupted()) {
				ZMsg msg = ZMsg.recvMsg(worker);
				ZFrame address = msg.pop();
				ZFrame content = msg.pop();
				assert (content != null);
				logAction(User.WORKER, Action.RCV, workerId, new String(content.getData()));
				msg.destroy();

				int replies = rand.nextInt(5);
				for (int reply = 0; reply < replies; reply++) {
					try {
						Thread.sleep(rand.nextInt(1000) + 1);
					} catch (InterruptedException e) {
					}
					address.send(worker, ZFrame.REUSE + ZFrame.MORE);
					String responseMsg = new String(content.getData()) + ": response from "+ workerId;
					logAction(User.WORKER, Action.SND, workerId, responseMsg);
					ZFrame response = new ZFrame(responseMsg);
					response.send(worker, ZFrame.REUSE);
					response.destroy();
				}
				address.destroy();
				content.destroy();
			}
			ctx.destroy();
		}
	}

	public static void main(String[] args) throws Exception {
		@SuppressWarnings("resource")
		ZContext ctx = new ZContext();
//		new Thread(new client_task()).start();
//		new Thread(new client_task()).start();
		new Thread(new client_task()).start();
		new Thread(new server_task()).start();

		Thread.sleep(5 * 1000);
		ctx.destroy();
	}
	static void logAction(User user, Action action, String userId, String message) {
		System.out.println(String.format("%s %s %s {%s}", user, userId, action, message));
	}
}