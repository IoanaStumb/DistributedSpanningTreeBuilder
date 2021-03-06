package threads;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import cluster.SpanningTree;

public class ClientsNotifier extends Thread {

	private volatile boolean shouldRun = true;
	private int creatorNodePort;
	private AtomicBoolean creatorIsRoot;
	private Map<Integer, SpanningTree> spanningTrees;
	private Queue<SocketAddress> waitingClients;
	private int checkForClientsAfterMs;

	private DatagramSocket externalSocket = null;
	private DatagramPacket response = null;

	public ClientsNotifier(String name, int creatorNodePort, AtomicBoolean creatorIsRoot, Map<Integer, SpanningTree> spanningTrees, 
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
		byte[] buffer = new byte[256];
		
		while (shouldRun) {
			try {
				// check if there is a root tree for this node
				if (creatorIsRoot.get()) {		
					
					// while we have clients, send tree
					while (waitingClients.size() > 0) {						
						
						SocketAddress client = waitingClients.peek();
						System.out.println("NOTIFYING CLIENT! " + client);

						// find tree and return the treeExpression
						SpanningTree spanningTree = spanningTrees.get(creatorNodePort);
						System.out.println("[" + this.getName() + ":" + this.creatorNodePort + "]: Tree expression: " + spanningTree.treeExpression);

						buffer = String.valueOf(spanningTree.treeExpression).getBytes();
						response = new DatagramPacket(buffer, buffer.length, client);
						
						externalSocket.send(response);
						waitingClients.poll();
					}					
				}
				
				Thread.sleep((long) checkForClientsAfterMs);
			} 
			catch (IOException | InterruptedException exception) {
				exception.printStackTrace();
				finish();
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
