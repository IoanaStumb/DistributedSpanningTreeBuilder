package threads;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import cluster.SpanningTree;

public class ClientsNotifier extends Thread {

	private volatile boolean shouldRun = true;
	private int creatorNodePort;
	private AtomicBoolean creatorIsRoot;
	private List<SpanningTree> spanningTrees;
	private Queue<SocketAddress> waitingClients;
	private int checkForClientsAfterMs;

	private DatagramSocket externalSocket = null;
	private DatagramPacket response = null;

	public ClientsNotifier(String name, int creatorNodePort, AtomicBoolean creatorIsRoot, List<SpanningTree> spanningTrees, 
			Queue<SocketAddress> waitingClients, DatagramSocket externalSocket, int checkForClientsAfterMs) {
		super(name);
		this.creatorNodePort = creatorNodePort;
		this.creatorIsRoot = creatorIsRoot;
		this.spanningTrees = spanningTrees;
		this.waitingClients = waitingClients;
		this.checkForClientsAfterMs = checkForClientsAfterMs;

		this.externalSocket = externalSocket;
	}

	@Override
	public void run() {
		// check periodically (variable for this) if there are waiting clients & a tree
		// has been built
		// if so, send the tree to the clients and clear the waiting clients list
		byte[] buffer = new byte[256];

		while (shouldRun) {
			try {
				if (waitingClients.size() > 0 && creatorIsRoot.get()) {
					// while we still have clients, send tree
					SocketAddress client = waitingClients.peek();

					// find tree and return the treeExpression
					String treeExpression = spanningTrees.stream()
							.filter(st -> st.id == creatorNodePort)
							.findFirst()
							.get().treeExpression;

					System.out.println("[" + this.getName() + ":" + this.creatorNodePort + "]: Tree expression: " + treeExpression);

					buffer = String.valueOf(treeExpression).getBytes();
					response = new DatagramPacket(buffer, buffer.length, client);
					
					externalSocket.send(response);
					waitingClients.poll();
				}
				
				// wait for the specified ms
			} 
			catch (IOException exception) {
				exception.printStackTrace();
				closeAll();
			}
		}
	}

	public void finish() {
		System.out.println("[" + this.getName() + ":" + this.creatorNodePort + "]: I am stopping.");
		closeAll();
	}

	private void closeAll() {
		this.shouldRun = false;
		if (this.externalSocket != null)
			this.externalSocket.close();
	}
}
