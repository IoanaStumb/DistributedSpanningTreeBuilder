package cluster;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
		
		addNeighbors();
		
		SpanningTreeParticipant spanningTreeParticipant = new SpanningTreeParticipant("SpanningTreeParticipant", internalPort, 
				isRootForTree, isBuildingTree, address, internalNeighborPorts, spanningTrees, internalSocket);
		SpanningTreeCleaner spanningTreeCleaner = new SpanningTreeCleaner("SpanningTreeCleaner", internalPort, isRootForTree,
				isBuildingTree, spanningTrees, 5000, 10000);
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
						buffer = "message".getBytes();
						SpanningTree spanningTree = spanningTrees.get(internalPort);
						
						for(Integer childPort : spanningTree.childrenPorts) {
							response = new DatagramPacket(buffer, buffer.length, address, childPort);
							internalSocket.send(response);
						}							
						break;
				};
			}
		} finally {
			externalSocket.close();
			internalSocket.close();
		}
	}

	private void addNeighbors() throws IOException {		
		List<String> lines = Files.readAllLines(Paths.get("topology.txt"), Charset.defaultCharset());
		
		for (String line : lines) {
			String[] tokens = line.split(":");
			
			if (internalPort == Integer.parseInt(tokens[0])) {				
				for (int i = 1; i < tokens.length; i++) {
					internalNeighborPorts.add(Integer.parseInt(tokens[i]));
				}
			}
		}
	}
	
	public static void main(String[] args) {
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
