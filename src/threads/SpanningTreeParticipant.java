package threads;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import cluster.Message;
import cluster.SpanningTree;

public class SpanningTreeParticipant extends Thread {

	private volatile boolean shouldRun = true;
	private int creatorNodePort;
	private AtomicBoolean creatorIsRoot;
	private AtomicBoolean creatorIsBuildingTree;
	private InetAddress address;
	private Set<Integer> internalNeighborPorts;
	private List<SpanningTree> spanningTrees;
	
	private DatagramSocket internalSocket = null;
	private DatagramPacket request, response = null;
	
	public SpanningTreeParticipant(String name, int creatorNodePort, AtomicBoolean creatorIsRoot, AtomicBoolean creatorIsBuildingTree, InetAddress address, 
			Set<Integer> internalNeighborPorts, List<SpanningTree> spanningTrees, DatagramSocket internalSocket) throws SocketException {
		super(name);
		this.creatorNodePort = creatorNodePort;
		this.creatorIsRoot = creatorIsRoot;
		this.creatorIsBuildingTree = creatorIsBuildingTree;
		this.address = address;
		this.internalNeighborPorts = internalNeighborPorts;
		this.spanningTrees = spanningTrees;
		
		this.internalSocket = internalSocket;
	}
		
	@Override
	public void run() {
		try {
			byte[] buffer = new byte[256];
			request = new DatagramPacket(buffer, buffer.length);
			
			while (shouldRun) {
				internalSocket.receive(request);
							
				int senderPort = request.getPort();	
				Message message = Message.convertToMessage(request.getData(), request.getOffset(), request.getLength());
				
				System.out.println("Received message: " + message.action + " from node: " + senderPort);
				
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
					case "join-tree":
						// if I have no parent for this spanning tree, set the parent
						if (spanningTree.parentPort == -1) {
							spanningTree.parentPort = senderPort;
							
							// send "p" to my parent for this spanning tree
							buffer = new Message(message.identifier, "parent").toBytes();
							
							response = new DatagramPacket(buffer, buffer.length, address, senderPort);
							internalSocket.send(response);
							
							// if I am leaf with only one neighbor, I should stop
							if (internalNeighborPorts.size() == 1 && internalNeighborPorts.iterator().next() == spanningTree.parentPort) {								
								spanningTree.receivedResponsesFromAllNeighbors = true;
								
								// send "f [expressionTree]" to parent to signal finished
								String expressionTree = spanningTree.buildAndSetTreeExpression(creatorNodePort);
								buffer = new Message(message.identifier, "finished", expressionTree).toBytes();
								
								response = new DatagramPacket(buffer, buffer.length, address, senderPort);
								internalSocket.send(response);
							}
							else {
								// else send "m" to neighbors	
								buffer = new Message(message.identifier, "join-tree").toBytes();
								
								for(Integer neighborPort : internalNeighborPorts) {
									if (neighborPort != senderPort) {
										response = new DatagramPacket(buffer, buffer.length, address, neighborPort);
										internalSocket.send(response);
									}
								}	
							}
						}
						else {
							// send "a" to senderPort
							buffer = new Message(message.identifier, "already").toBytes();
							
							response = new DatagramPacket(buffer, buffer.length, address, senderPort);
							internalSocket.send(response);
						}
						break;
						
					case "parent":
						// add sender to my kids
						spanningTree.childrenPorts.add(senderPort);
						
						// if kids + others = all neighbors I should stop
						if (sentToAllNeighbors(spanningTree)) {
							System.out.println("I finished my neighbors!");
							spanningTree.receivedResponsesFromAllNeighbors = true;
						}					
						break;
						
					case "already":
						// add sender to my others
						spanningTree.otherPorts.add(senderPort);
						
						// if kids + others = all neighbors
						if (sentToAllNeighbors(spanningTree)) {
							System.out.println("I finished my neighbors!");
							spanningTree.receivedResponsesFromAllNeighbors = true;
							
							// if I don't have any kids, I should also stop
							if (spanningTree.childrenPorts.size() == 0) {
								
								// send "f [expressionTree]" to parent to signal finished
								String expressionTree = spanningTree.buildAndSetTreeExpression(creatorNodePort);
								buffer = new Message(message.identifier, "finished", expressionTree).toBytes();
								
								response = new DatagramPacket(buffer, buffer.length, address, spanningTree.parentPort);
								internalSocket.send(response);
							}
						}
						break;
						
					case "finished":
						System.out.println("Received expression tree: " + message.optionalContent);
						
						spanningTree.finishedChildrenResponses.put(senderPort, message.optionalContent);

						// if all my kids finished
						if (spanningTree.receivedResponsesFromAllNeighbors && allChildrenFinished(spanningTree)) {
							System.out.println("All my kids finished, good job!");
							
							// build and set my expression for this tree
							String expressionTree = spanningTree.buildAndSetTreeExpression(creatorNodePort);
							System.out.println("My final expression for this tree: " + expressionTree);
							
							// if I am not the tree root => send the expression for this spanning tree to my parent						
							if (creatorNodePort != spanningTree.id) {								
								buffer = new Message(message.identifier, "finished", expressionTree).toBytes();
								
								response = new DatagramPacket(buffer, buffer.length, address, spanningTree.parentPort);
								internalSocket.send(response);
							}
							// if I am the tree root => set my check values 
							else {
								creatorIsRoot.set(true);
								creatorIsBuildingTree.set(false);
							}
						}
						break;
				};
			}
		}
		catch(IOException exception) {
			exception.printStackTrace();
			finish();
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
		Set<Integer> temporaryChildrenPorts = new HashSet<>(spanningTree.childrenPorts);
		temporaryChildrenPorts.removeAll(temporaryFinishedChildrenPorts);	
		
		return temporaryChildrenPorts.size() == 0;
	}
	
	private void closeAll() {
		this.shouldRun = false;
		if (this.internalSocket != null)
			this.internalSocket.close();
	}
}
