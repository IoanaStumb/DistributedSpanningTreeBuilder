package client;

import java.net.*;
import java.util.Arrays;
import java.io.*;

public class Client {

	public static void main(String[] args) throws IOException {

		InetAddress address = InetAddress.getByName("127.0.0.1");
		DatagramSocket socket = null;
		DatagramPacket packet = null;
		byte[] buffer = null;
		
		SpanningTreeReceiver spanningTreeReceiver = null;

		try {
			BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
			socket = new DatagramSocket();
			
			// start the tree receiver
			spanningTreeReceiver = new SpanningTreeReceiver("SpanningTreeReceiver", socket);
			spanningTreeReceiver.start();
			
			String userInput;
            while (true) {
            	userInput = stdIn.readLine();
            	
            	if (userInput.equals("close")) {
            		break;
            	}
            	
            	String[] tokens = userInput.split(" ");   
            	int port = Integer.parseInt(tokens[0]);
            	String message = tokens[1];
            	
            	if (!Arrays.asList("request-tree", "send-message").contains(message)) {
            		System.out.println("Action not allowed!");
            		break;
            	}
            	
            	if (!message.equals("send-message")) {
            		buffer = message.getBytes();
            	}
            	else {
            		buffer = new String(message + " " + tokens[2]).getBytes();
            	}
            	        	
            	packet = new DatagramPacket(buffer, buffer.length, address, port);
            	socket.send(packet);
            }
		} 
		catch (NumberFormatException exception) {
			exception.printStackTrace();
		}
		finally {
			spanningTreeReceiver.finish();
			socket.close();
		}
	}
}
