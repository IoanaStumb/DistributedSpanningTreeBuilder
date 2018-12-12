package cluster;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

public class InitialNode {
	private int port;
	private int parentPort = -1;
	private InetAddress address = null;
	private Set<Integer> neighborPorts = new HashSet<>();
	private Set<Integer> childrenPorts = new HashSet<>();
	private Set<Integer> otherPorts = new HashSet<>();
	
	private DatagramSocket socket = null;
	private DatagramPacket request, response = null;
	
	public InitialNode(int port) throws IOException {
		this.port = port;
		
		address = InetAddress.getByName("127.0.0.1");
		socket = new DatagramSocket(this.port);
		
		switch(this.port) {
			case 12000:
				neighborPorts.add(12001);
				neighborPorts.add(12002);
				break;
			case 12001:
				neighborPorts.add(12003);
				neighborPorts.add(12004);
				break;
			case 12002:
				neighborPorts.add(12001);
				neighborPorts.add(12003);
				break;
			case 12003:
				neighborPorts.add(12006);
				break;
			case 12004:
				neighborPorts.add(12005);
				break;
			case 12005:
				neighborPorts.add(12004);
				break;
			case 12006:
				neighborPorts.add(12003);
				break;
		}
	}
	
	public void runServer() throws IOException {
		
		try {
			byte[] buffer = new byte[256];
			request = new DatagramPacket(buffer, buffer.length);
					
			while (true) {
				socket.receive(request);
							
				int senderPort = request.getPort();
				String message = new String(request.getData(), request.getOffset(), request.getLength());
				
				System.out.println("Adresa si port client: " + address + ":" + senderPort + ", mesaj: " + message);
				
				// flooding. dfs would be better actually?!
				switch(message) {
					case "s":
						// start the algorithm
						if (parentPort == -1) {
							parentPort = port;
							buffer = String.valueOf("m").getBytes();
							
							for(Integer neighborPort : neighborPorts) {
								response = new DatagramPacket(buffer, buffer.length, address, neighborPort);
								socket.send(response);
							}
							
							// should wait for answer here and send it back to this senderPort?
						}							
						break;
					
					case "m":
						// if I have no parent, set my parent
						if (parentPort == -1) {
							parentPort = senderPort;
							
							// send "p" to parent
							buffer = String.valueOf("p").getBytes();
							
							response = new DatagramPacket(buffer, buffer.length, address, senderPort);
							socket.send(response);
							
							// if I am leaf with only one neighbor, I should stop -> see issue below
							if (neighborPorts.size() == 1 && neighborPorts.iterator().next() == parentPort) {
								
								// send "f" to parent to signal finished
								buffer = String.valueOf("f").getBytes();
								
								response = new DatagramPacket(buffer, buffer.length, address, parentPort);
								socket.send(response);
							}
							else {
								// else send "m" to neighbors	
								buffer = String.valueOf("m").getBytes();
								
								for(Integer neighborPort : neighborPorts) {
									if (neighborPort != senderPort) {
										response = new DatagramPacket(buffer, buffer.length, address, neighborPort);
										socket.send(response);
									}
								}	
							}
						}
						else {
							// send "a" to senderPort
							buffer = String.valueOf("a").getBytes();
							
							response = new DatagramPacket(buffer, buffer.length, address, senderPort);
							socket.send(response);
						}
						break;
						
					case "p":
						// add sender to my kids
						childrenPorts.add(senderPort);
						
						// if kids + others = all neighbors I should stop -> send message to parent to send it off? or how?
						if (sentToAllNeighbors()) {
							System.out.println("I finished my neighbors!");
							
							// send "f" to parent to signal finished
							buffer = String.valueOf("f").getBytes();
							
							response = new DatagramPacket(buffer, buffer.length, address, parentPort);
							socket.send(response);
						}					
						break;
						
					case "a":
						// add sender to my others
						otherPorts.add(senderPort);
						
						// if kids + others = all neighbors -> see issue above
						if (sentToAllNeighbors()) {
							System.out.println("I finished my neighbors!");
							
							// send "f" to parent to signal finished
							buffer = String.valueOf("f").getBytes();
							
							response = new DatagramPacket(buffer, buffer.length, address, parentPort);
							socket.send(response);
						}						
						break;
						
					case "f":
						break;
				};
			}
		} finally {
			socket.close();
		}
	}
	
	private Boolean sentToAllNeighbors() {
		
		Set<Integer> temporaryNeighborPorts = new HashSet<>(neighborPorts);
		temporaryNeighborPorts.removeAll(childrenPorts);
		temporaryNeighborPorts.removeAll(otherPorts);
		temporaryNeighborPorts.remove(parentPort);
		
		return temporaryNeighborPorts.size() == 0;
	}
	
	public static void main(String[] args) {
		
		// port is received by parameter
		// each node should have a list of its neighbors
		// if I am the starter, send a message to my neighbors
		// then enter a while loop
		// while what? -> while algorithm not finished? -> somebody needs to send a finished message
		// and start receiving and processing messages
		
		InitialNode ds;
		
		try {
			int port = Integer.parseInt(args[0]);
			ds = new InitialNode(port);
			ds.runServer();
			
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
}
