package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class SpanningTreeReceiver extends Thread {
	
	private volatile boolean shouldRun = true;
	private DatagramSocket socket = null;
	private DatagramPacket request = null;
	
	public SpanningTreeReceiver(String name, DatagramSocket socket) {
		super(name);
		this.socket = socket;
	}
	
	@Override
	public void run() {
		try {
			byte[] buffer = new byte[256];
			request = new DatagramPacket(buffer, buffer.length);
			
			while (shouldRun) {
				socket.receive(request);
				
				int senderPort = request.getPort();	
				String tree = new String(request.getData(), request.getOffset(), request.getLength());
				
				System.out.println("Node: " + senderPort + " sent message: ");
				System.out.println(tree);
			}
		}
		catch(IOException exception) {
			exception.printStackTrace();
			finish();
		}
	}
	
	public void finish() {
		System.out.println("[" + this.getName() + "]: I am stopping.");		
		closeAll();
	}
	
	private void closeAll() {
		this.shouldRun = false;
		if (this.socket != null)
			this.socket.close();
	}
}
