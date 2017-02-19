import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

public class MyServer extends Thread{
	private ServerSocket serverSocket;
	private Socket server;
	private static int portNum = 8084;

	private ArrayList<Frame> successfulFrames;
	private ArrayList<Frame> bufferFrames; 
	int nextFrameIndex;
	private Frame frame;
	private boolean running = false;

	private DataInputStream dataInputStream;
	private DataOutputStream dataOutputStream;
	private ObjectInputStream objectInputStream;
	private ObjectOutputStream objectOutputStream;

	public MyServer(int port) {
		successfulFrames = new ArrayList<>();
		bufferFrames = new ArrayList<>();
		nextFrameIndex = 1;
		System.out.println("-------------------------Server--------------------------------");
		try {
			serverSocket = new ServerSocket(portNum);
			serverSocket.setSoTimeout(5000);
			System.out.println("Waiting for client on port " + serverSocket.getLocalPort() + "..");
			server = serverSocket.accept();
			System.out.println("Just connected to " + server.getRemoteSocketAddress());
			System.out.println();

			// Create Data I/O streams
			dataInputStream = new DataInputStream(server.getInputStream());
			dataOutputStream = new DataOutputStream(server.getOutputStream());

			// Create Object I/O Streams
			objectInputStream = new ObjectInputStream(dataInputStream);
			objectOutputStream = new ObjectOutputStream(dataOutputStream);

			running = true;
		} catch (SocketException se) {
			se.printStackTrace();
		}
		catch (SocketTimeoutException ste) {
			running = false;
		} catch (IOException io) {
			io.printStackTrace();
		}
	}

	@Override
	public void run() {
		while(running) {
			// Welcome client to Server
			//dataOutputStream.writeUTF("SERVER: You have succesfully connected");
			//
			//				// Create object input stream from this data stream
			//				ObjectInputStream objectInputStream = new ObjectInputStream(dataInputStream);

			// Try accessing the object
			try {
				frame = (Frame)objectInputStream.readObject();
				System.out.println("Frame Received from client - Checking CRC");
				byte[] ack = new byte[1];
				if(frame.checkCRC()){
					successfulFrames.add(frame);
					nextFrameIndex++;
					ack[0] = 'a';
				} else {
					successfulFrames.add(frame);
					ack[0] = 'n';
				}

				// Return the ack
				Frame returnFrame = new Frame(frame.getSequenceNumber(),(short)1, ack);
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(dataOutputStream);
				objectOutputStream.writeObject(returnFrame);

			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException io) {
				System.out.println("IO exception");
				io.printStackTrace();
			}
		}

		System.out.println("Finished Receiving");

		// Write to file
		/*		PrintWriter out;
		try {
			out = new PrintWriter("src/output.txt");
			String s = "";
			System.out.println("Frame length = " + successfulFrames.size());
			for(Frame frame : successfulFrames) {
				System.out.println("Frame " + frame.getSequenceNumber() + ": " + frame.getData());
				s += frame.getData();
			}
			System.out.println(s);
			out.print(s);
			out.close();
		} catch(IOException e) {
			System.out.println(e);
		}*/
	}



	public static void main(String[] args) {
		Thread t = new MyServer(8083);
		t.start();
	}
}
