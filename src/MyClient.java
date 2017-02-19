import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;




public class MyClient {
	private static int portNum = 8084;
	private static String serverName = "localhost";
	private static int framesSent = 1;


	public static void main(String[] args) throws IOException{

		FileInputStream in = null;
		byte[] bytes = new byte[8];
		short byteCounter = 0;
		short sequenceNumber = 1;
		try {
			in = new FileInputStream("src/data.txt");
			int c;
			while ((c = in.read()) != -1) {
				bytes[byteCounter] = (byte)c;
				byteCounter++;
				if(byteCounter == 8){
					System.out.println("------------------------------ Sending Frame " + framesSent + " ------------------------------");
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
				System.out.println("------------------------------ Sending Frame " + framesSent + " ------------------------------");
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
		}
	}
	
	private static void sendFrame(Frame frame){
		try{
			Socket client = new Socket(serverName, portNum);

			DataInputStream dataInputStream = new DataInputStream(client.getInputStream());
			DataOutputStream dataOutputStream = new DataOutputStream(client.getOutputStream());

			// Retrieve Welcome message
			System.out.println();
			System.out.println(dataInputStream.readUTF());
			System.out.println();

			// Send the frame
			System.out.println("Attempting to send the frame : '" + frame.getData() + "'");
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(dataOutputStream);
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
	

}
