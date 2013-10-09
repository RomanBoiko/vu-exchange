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
	private static Random rand = new Random(System.nanoTime());

	private static class client_task implements Runnable {

		public void run() {
			@SuppressWarnings("resource")
			ZContext ctx = new ZContext();
			Socket client = ctx.createSocket(ZMQ.DEALER);

			String identity = String.format("%04X-%04X", rand.nextInt(),
					rand.nextInt());
			client.setIdentity(identity.getBytes());
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
						msg.getLast().print(identity);
						msg.destroy();
					}
				}
				client.send(String.format("request #%d", ++requestNbr), 0);
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
				new Thread(new server_worker(ctx)).start();

			ZMQ.proxy(frontend, backend, null);

			ctx.destroy();
		}
	}

	private static class server_worker implements Runnable {
		private ZContext ctx;

		public server_worker(ZContext ctx) {
			this.ctx = ctx;
		}

		public void run() {
			Socket worker = ctx.createSocket(ZMQ.DEALER);
			worker.connect("inproc://backend");

			while (!Thread.currentThread().isInterrupted()) {
				ZMsg msg = ZMsg.recvMsg(worker);
				ZFrame address = msg.pop();
				ZFrame content = msg.pop();
				assert (content != null);
				msg.destroy();

				int replies = rand.nextInt(5);
				for (int reply = 0; reply < replies; reply++) {
					try {
						Thread.sleep(rand.nextInt(1000) + 1);
					} catch (InterruptedException e) {
					}
					address.send(worker, ZFrame.REUSE + ZFrame.MORE);
					content.send(worker, ZFrame.REUSE);
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
		new Thread(new client_task()).start();
		new Thread(new client_task()).start();
		new Thread(new client_task()).start();
		new Thread(new server_task()).start();

		Thread.sleep(5 * 1000);
		ctx.destroy();
	}
}