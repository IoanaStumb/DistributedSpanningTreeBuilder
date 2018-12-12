package threads;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import cluster.Message;
import cluster.SpanningTree;

public class SpanningTreeParticipant extends Thread {

	private volatile boolean shouldRun = true;
	private int creatorNodePort;
	private volatile boolean creatorIsRoot;
	private volatile boolean creatorIsBuildingTree;
	private InetAddress address;
	private Set<Integer> internalNeighborPorts;
	private List<SpanningTree> spanningTrees;
	
	private DatagramSocket socket = null;
	private DatagramPacket request, response = null;
	
	public SpanningTreeParticipant(String name, int creatorNodePort, boolean creatorIsRoot, boolean creatorIsBuildingTree, InetAddress address, 
			Set<Integer> internalNeighborPorts, List<SpanningTree> spanningTrees, DatagramSocket internalSocket) throws SocketException {
		super(name);
		this.creatorNodePort = creatorNodePort;
		this.creatorIsRoot = creatorIsRoot;
		this.creatorIsBuildingTree = creatorIsBuildingTree;
		this.address = address;
		this.internalNeighborPorts = internalNeighborPorts;
		this.spanningTrees = spanningTrees;
		
		socket = internalSocket;
	}
	
	@Override
	public void run() {
		try {
			byte[] buffer = new byte[256];
			request = new DatagramPacket(buffer, buffer.length);
			
			while (shouldRun) {
				socket.receive(request);
							
				int senderPort = request.getPort();	
				Message message = Message.convertToMessage(request.getData(), request.getOffset(), request.getLength());
				
				System.out.println("[" + this.getName() + ":" + this.creatorNodePort + "]: Received message: " + message.action + " from node: " + senderPort);
				
				// check if we already have a tree started for this identifier 
				// if not, start it
				SpanningTree spanningTree = spanningTrees.stream()
						.filter(tree -> tree.id == message.identifier)
						.findAny()
						.orElse(null);
				
				if (spanningTree == null) {
					spanningTree = new SpanningTree(message.identifier);
					spanningTrees.add(spanningTree);
				}
				
				switch(message.action) {					
					case "m":
						// if I have no parent for this spanning tree, set the parent
						if (spanningTree.parentPort == -1) {
							spanningTree.parentPort = senderPort;
							
							// send "p" to my parent for this spanning tree
							buffer = new Message(message.identifier, "p").toBytes();
							
							response = new DatagramPacket(buffer, buffer.length, address, senderPort);
							socket.send(response);
							
							// if I am leaf with only one neighbor, I should stop
							if (internalNeighborPorts.size() == 1 && internalNeighborPorts.iterator().next() == spanningTree.parentPort) {								
								spanningTree.receivedResponsesFromAllNeighbors = true;
								
								// send "f [expressionTree]" to parent to signal finished
								String expressionTree = spanningTree.buildAndSetTreeExpression(creatorNodePort);
								buffer = new Message(message.identifier, "f", expressionTree).toBytes();
								
								response = new DatagramPacket(buffer, buffer.length, address, senderPort);
								socket.send(response);
							}
							else {
								// else send "m" to neighbors	
								buffer = new Message(message.identifier, "m").toBytes();
								
								for(Integer neighborPort : internalNeighborPorts) {
									if (neighborPort != senderPort) {
										response = new DatagramPacket(buffer, buffer.length, address, neighborPort);
										socket.send(response);
									}
								}	
							}
						}
						else {
							// send "a" to senderPort
							buffer = new Message(message.identifier, "a").toBytes();
							
							response = new DatagramPacket(buffer, buffer.length, address, senderPort);
							socket.send(response);
						}
						break;
						
					case "p":
						// add sender to my kids
						spanningTree.childrenPorts.add(senderPort);
						
						// if kids + others = all neighbors I should stop
						if (sentToAllNeighbors(spanningTree)) {
							System.out.println("I finished my neighbors!");
							spanningTree.receivedResponsesFromAllNeighbors = true;
							
							// send "lalala" to parent to signal finished
							// TODO: remove after finishing "f" case
							if (creatorNodePort != spanningTree.parentPort) {
								buffer = new Message(message.identifier, "lalala").toBytes();
								
								response = new DatagramPacket(buffer, buffer.length, address, spanningTree.parentPort);
								socket.send(response);
							}
						}					
						break;
						
					case "a":
						// add sender to my others
						spanningTree.otherPorts.add(senderPort);
						
						// if kids + others = all neighbors
						if (sentToAllNeighbors(spanningTree)) {
							System.out.println("I finished my neighbors!");
							spanningTree.receivedResponsesFromAllNeighbors = true;
							
							// send "lalala" to parent to signal finished
							// TODO: remove after finishing "f" case
							if (creatorNodePort != spanningTree.parentPort) {
								buffer = new Message(message.identifier, "lalala").toBytes();

								response = new DatagramPacket(buffer, buffer.length, address, spanningTree.parentPort);
								socket.send(response);
							}
						}
						break;
						
					case "f":
						spanningTree.finishedChildrenResponses.put(senderPort, message.optionalContent);
						
						// if all my kids finished and I am the root, I should signal this somehow to the clients
						if (spanningTree.receivedResponsesFromAllNeighbors && allChildrenFinished(spanningTree)) {
							System.out.println("All my kids finished, good job!");
							
							// build the expression tree for this spanning tree and send it to parent
							String expressionTree = spanningTree.buildAndSetTreeExpression(creatorNodePort);
							buffer = new Message(message.identifier, "f", expressionTree).toBytes();
							
							response = new DatagramPacket(buffer, buffer.length, address, spanningTree.parentPort);
							socket.send(response);
							
							if (creatorNodePort == spanningTree.id) {
								creatorIsRoot = true;
								creatorIsBuildingTree = false;
							}
						}
						break;
				};
			}
		}
		catch(IOException exception) {
			exception.printStackTrace();
			closeAll();
		}
	}
	
	public void finish() {
		System.out.println("[" + this.getName() + ":" + this.creatorNodePort + "]: I am stopping.");		
		closeAll();
	}
	
	private boolean sentToAllNeighbors(SpanningTree spanningTree) {
		
		Set<Integer> temporaryNeighborPorts = new HashSet<>(this.internalNeighborPorts);
		temporaryNeighborPorts.removeAll(spanningTree.childrenPorts);
		temporaryNeighborPorts.removeAll(spanningTree.otherPorts);
		temporaryNeighborPorts.remove(spanningTree.parentPort);
		
		return temporaryNeighborPorts.size() == 0;
	}
	
	private boolean allChildrenFinished(SpanningTree spanningTree) {
		
		Set<Integer> temporaryFinishedChildrenPorts = 
				spanningTree.finishedChildrenResponses.entrySet().stream()
				.map(entry -> entry.getKey())
				.collect(Collectors.toSet());
		temporaryFinishedChildrenPorts.removeAll(spanningTree.childrenPorts);
		
		return temporaryFinishedChildrenPorts.size() == 0;
	}
	
	private void closeAll() {
		this.shouldRun = false;
		if (this.socket != null)
			this.socket.close();
	}
}
