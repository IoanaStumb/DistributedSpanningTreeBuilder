package client;

import java.net.*;
import java.io.*;

public class Client {

	public static void main(String[] args) throws IOException {

		InetAddress address = InetAddress.getByName("127.0.0.1");
		int port = 13000;
		DatagramSocket socket = null;
		DatagramPacket packet = null;
		byte[] buf = null;

		try {
			socket = new DatagramSocket();

			String s = "request-tree";
			buf = s.getBytes();
			packet = new DatagramPacket(buf, buf.length, address, port);
			
			socket.send(packet);
		} finally {
			socket.close();
		}
	}
}
