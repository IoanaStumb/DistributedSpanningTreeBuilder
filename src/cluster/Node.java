package cluster;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import threads.ClientsNotifier;
import threads.SpanningTreeCleaner;
import threads.SpanningTreeParticipant;

public class Node {
	private int externalPort;
	private int internalPort;
	private InetAddress address;
	private Set<Integer> internalNeighborPorts;
	
	private AtomicBoolean isRootForTree;
	private AtomicBoolean isBuildingTree;
	private Map<Integer, SpanningTree> spanningTrees;
	private Queue<SocketAddress> waitingClients;
	
	private DatagramSocket externalSocket, internalSocket;
	private DatagramPacket request, response;
	
	public Node() {
		spanningTrees = new ConcurrentHashMap<Integer, SpanningTree>();
		internalNeighborPorts = new HashSet<>();
		waitingClients = new ConcurrentLinkedQueue<>();
		isRootForTree = new AtomicBoolean(false);
		isBuildingTree = new AtomicBoolean(false);
	}

	public void initialize(int externalPort, int internalPort) throws IOException {
		this.externalPort = externalPort;
		this.internalPort = internalPort;
		
		address = InetAddress.getByName("127.0.0.1");
		externalSocket = new DatagramSocket(this.externalPort);
		internalSocket = new DatagramSocket(this.internalPort);
		
		// replace with neighbor initialization routine
		switch(this.internalPort) {
			case 12000:
				internalNeighborPorts.add(12001);
				internalNeighborPorts.add(12002);
				break;
			case 12001:
				internalNeighborPorts.add(12003);
				internalNeighborPorts.add(12004);
				break;
			case 12002:
				internalNeighborPorts.add(12001);
				internalNeighborPorts.add(12003);
				break;
			case 12003:
				internalNeighborPorts.add(12006);
				break;
			case 12004:
				internalNeighborPorts.add(12005);
				break;
			case 12005:
				internalNeighborPorts.add(12004);
				break;
			case 12006:
				internalNeighborPorts.add(12003);
				break;
		}
		
		SpanningTreeParticipant spanningTreeParticipant = new SpanningTreeParticipant("SpanningTreeParticipant", internalPort, 
				isRootForTree, isBuildingTree, address, internalNeighborPorts, spanningTrees, internalSocket);
		SpanningTreeCleaner spanningTreeCleaner = new SpanningTreeCleaner("SpanningTreeCleaner", internalPort, isRootForTree,
				isBuildingTree, spanningTrees, 2000, 5000);
		ClientsNotifier clientsNotifier = new ClientsNotifier("ClientsNotifier", internalPort, isRootForTree, spanningTrees,
				waitingClients, externalSocket, 2000);
		
		spanningTreeParticipant.start();
		spanningTreeCleaner.start();
		clientsNotifier.start();
	}
	
	public void runServer() throws IOException {
		try {
			byte[] buffer = new byte[256];
			request = new DatagramPacket(buffer, buffer.length);
					
			while (true) {
				externalSocket.receive(request);
				
				SocketAddress senderAddress = request.getSocketAddress();
				String message = new String(request.getData(), request.getOffset(), request.getLength());
				
				System.out.println("Client address and port: " + senderAddress + ", message: " + message);
				
				switch(message) {
					case "request-tree":
						// add clients to waiting queue, ClientsNotifier thread will deal with them
						waitingClients.add(senderAddress);
						
						if (!isRootForTree.get() && !isBuildingTree.get()) {
							System.out.println("[Node " + this.internalPort + "]: Starting to build my spanning tree.");
							isBuildingTree.set(true);
						
							// start building own tree
							SpanningTree myTree = new SpanningTree(internalPort);
							myTree.parentPort = internalPort;
							spanningTrees.put(internalPort, myTree);
							
							buffer = new Message(internalPort, "join-tree").toBytes();
							for(Integer neighborPort : internalNeighborPorts) {
								response = new DatagramPacket(buffer, buffer.length, address, neighborPort);
								internalSocket.send(response);
							}
						}
						break;
					
					case "send-message":
						// try to send message to node from tree
						break;
				};
			}
		} finally {
			externalSocket.close();
			internalSocket.close();
		}
	}
	
	public static void main(String[] args) {
		
		// port is received by parameter
		// each node should have a list of its neighbors
		// if I am the starter, send a message to my neighbors
		// then enter a while loop
		// while what? -> while algorithm not finished? -> somebody needs to send a finished message
		// and start receiving and processing messages
		
		Node clusterNode;
		
		try {
			int externalPort = Integer.parseInt(args[0]);
			int internalPort = Integer.parseInt(args[1]);
			
			clusterNode = new Node();
			clusterNode.initialize(externalPort, internalPort);
			clusterNode.runServer();
			
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
}
