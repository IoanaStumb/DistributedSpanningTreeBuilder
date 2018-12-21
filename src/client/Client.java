package client;

import java.net.*;
import java.io.*;

public class Client {

	public static void main(String[] args) throws IOException {

		InetAddress address = InetAddress.getByName("127.0.0.1");
		DatagramSocket socket = null;
		DatagramPacket packet = null;
		byte[] buffer = null;

		try {
			BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
			socket = new DatagramSocket();
			
			String userInput;
            while ((userInput = stdIn.readLine()) != null) {
            	String[] tokens = userInput.split(" ");
            	String message = tokens[0];
            	int port = Integer.parseInt(tokens[1]);
            	
            	buffer = message.getBytes();
            	packet = new DatagramPacket(buffer, buffer.length, address, port);
//                out.println(userInput);
//                System.out.println("echo: " + in.readLine());
            }
			socket.send(packet);
		} finally {
			socket.close();
		}
	}
}
