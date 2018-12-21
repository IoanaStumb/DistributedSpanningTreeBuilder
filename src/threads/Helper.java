package threads;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ThreadLocalRandom;

public class Helper {
	
	public static void delayedInternalSocketSend(DatagramSocket internalSocket, 
			DatagramPacket response) throws IOException {
		
		try {
			int randomMs = ThreadLocalRandom.current().nextInt(500, 1500 + 1);
			System.out.println("Delaying messages for " + randomMs + "ms");
			
			Thread.sleep((long) randomMs);
		} 
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		internalSocket.send(response);
	}
}
