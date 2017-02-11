


import java.io.DataOutputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.annotation.Generated;


public class MyClient {
	private static int portNum = 8084;
	//private static byte[] data = {'a','b','c','d','e','f','g','h'};
	private static byte[] data = {1,2,3,4,5,6,7,8};
	
	
	public static void main(String[] args) {
		System.out.println("-----------------Client-----------------------");
		
		Frame frame = new Frame("textfile.txt");
		String sequenceNumberBinary = frame.getSequenceNumberBinary();
		String payloadLengthBinary = frame.getPayloadLengthBinary();
		String dataBinary = frame.getDataBinary();
		String originalBinary = frame.getOriginalBinary();
		String remainderBinary = frame.getRemainderBinary();
		String fullBinary = frame.getFullBinary();
		
		System.out.println("Sequence Number \t" + sequenceNumberBinary);
		System.out.println("Payload length \t\t" + payloadLengthBinary);
		System.out.println("DataString \t\t" + dataBinary);
		System.out.println("Original Binary \t" + originalBinary);
		System.out.println("Remainder String \t" + remainderBinary);
		System.out.println("Full Binary \t\t" + fullBinary);
		System.out.println();
		System.out.println();
		
		System.out.println("Checking frame on Client Side (Debug)");
		frame.checkCRC();
		
		String serverName = "localhost";
		try{
			System.out.println("Attempting to connect to " + serverName + " on port " + portNum);
			Socket client = new Socket(serverName, portNum);
			System.out.println("Connected to " + client.getRemoteSocketAddress());
			
			OutputStream outToServer = client.getOutputStream();
			DataOutputStream out = new DataOutputStream(outToServer);
			
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(out);
			objectOutputStream.writeObject(frame);
			
			System.out.println("Data sent");
			client.close();
			

		} catch (Exception exception){
			System.out.println("Sending data failed - " + exception);
		}
	}
}
