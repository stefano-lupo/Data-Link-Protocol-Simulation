import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Random;




public class MyClient {
	private static int portNum = 8084;
	private static String serverName = "localhost";
	private static int framesSent = 1;


	public static void main(String[] args) throws IOException{
		System.out.println("-----------------Client-----------------------");

		FileInputStream in = null;
		FileOutputStream out = null;
		byte[] bytes = new byte[8];
		short byteCounter = 0;
		short sequenceNumber = 0;
		try {
			in = new FileInputStream("src/data.txt");
			//out = new FileOutputStream("src/output.txt");
			int c;
			while ((c = in.read()) != -1) {
				bytes[byteCounter] = (byte)c;
				byteCounter++;
				if(byteCounter == 8){
					Frame frame = new Frame(sequenceNumber, byteCounter, bytes);
					sendFrame(frame);
					//out.write(bytes);
					sequenceNumber++;
					byteCounter = 0;
				}
			}
		} finally {
			// If bytes remaining
			if(byteCounter != 0){
				System.out.println("Remaining bytes = " + byteCounter);
				byte[] remainingBytes = new byte[byteCounter];
				for(int i=0;i<byteCounter;i++) {
					remainingBytes[i] = bytes[i];
				}
				Frame frame  = new Frame(sequenceNumber, byteCounter, remainingBytes);
				sendFrame(frame);
			}
			if (in != null) {
				in.close();
			}
//			if(out!=null){
//				out.close();
//			}
		}


		// Create frame from text file (auto computes crc)
		/*Frame frame = new Frame("textfile.txt");

		String sequenceNumberBinary = frame.getSequenceNumberBinary();
		String payloadLengthBinary = frame.getPayloadLengthBinary();
		String dataBinary = frame.getDataBinary();
		String originalBinary = frame.getOriginalBinary();
		String remainderBinary = frame.getRemainderBinary();
		String fullBinary = frame.getFullBinary();

		System.out.println();
		System.out.println("Sequence Number \t" + sequenceNumberBinary);
		System.out.println("Payload length \t\t" + payloadLengthBinary);
		System.out.println("DataString \t\t" + dataBinary);
		System.out.println("Original Binary \t" + originalBinary);
		System.out.println("Remainder String \t" + remainderBinary);
		System.out.println("Full Binary \t\t" + fullBinary);
		System.out.println();
		System.out.println();

		//frame.checkCRC();


		// Connect to Server and send data
		try{
			System.out.println("Attempting to connect to " + serverName + " on port " + portNum);
			Socket client = new Socket(serverName, portNum);
			System.out.println("Connected to " + client.getRemoteSocketAddress());

			DataInputStream dataInputStream = new DataInputStream(client.getInputStream());
			DataOutputStream dataOutputStream = new DataOutputStream(client.getOutputStream());

			// Retrieve Welcome message
			System.out.println();
			System.out.println(dataInputStream.readUTF());
			System.out.println();

			// Send the frame
			System.out.println("Attempting to send the frame");
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(dataOutputStream);
			objectOutputStream.writeObject(frame);
			System.out.println("Frame sent to server");


			System.out.println();
			System.out.println("Awaiting Server Response..");
			System.out.println(dataInputStream.readUTF());
			System.out.println(dataInputStream.readUTF());

			client.close();


		} catch (Exception exception){
			System.out.println("Sending data failed - " + exception);
		}*/
	}
	
	private static void sendFrame(Frame frame){
		try{
			//System.out.println("Attempting to connect to " + serverName + " on port " + portNum);
			Socket client = new Socket(serverName, portNum);
			//System.out.println("Connected to " + client.getRemoteSocketAddress());

			DataInputStream dataInputStream = new DataInputStream(client.getInputStream());
			DataOutputStream dataOutputStream = new DataOutputStream(client.getOutputStream());

			// Retrieve Welcome message
			System.out.println();
			System.out.println(dataInputStream.readUTF());
			System.out.println();

			// Send the frame
			System.out.println("Attempting to send the frame");
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(dataOutputStream);
			gremlin(frame);
			objectOutputStream.writeObject(frame);
			framesSent++;
			System.out.println("Frame sent to server");


			System.out.println();
			System.out.println("Awaiting Server Response..");
			System.out.println(dataInputStream.readUTF());
			System.out.println(dataInputStream.readUTF());
			System.out.println();
			System.out.println();

			client.close();


		} catch (Exception exception){
			System.out.println("Sending data failed - " + exception);
		}
	}
	
	private static void gremlin(Frame frame){
		if(new Random().nextInt(10) > 5 ){
			System.out.println("GREMLIN on frame " + framesSent);
			frame.setRemainder((short)0xffff);
		}
	}
}
