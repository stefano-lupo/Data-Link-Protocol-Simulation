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


	public static void main(String[] args) {
		MyServer myServer = new MyServer(8083);
		
		myServer.start();										// Transmitter thread
		myServer.listen();										// Listening Thread


		// Once finished listening: Tidy up
		try{
			System.out.println("Terminating Connection");
			myServer.server.close();
			myServer.serverSocket.close();
		} catch (Exception e) {
			System.out.println("Exception closing sockets");
		}
	
		// Write result to file
		myServer.writeToFile();
		
	}


	ServerSocket serverSocket;
	Socket server;
	private static int portNum = 8084;
	private static final int WINDOW_SIZE = 2;
	private static final int TRANSMITER_SLEEP_TIME = 500;		// Thread Sleep time before checking buffer
	private static final int LISTENER_TIMEOUT_TIME = 5000;		// How long to wait for frame on input stream before shutting down

	private ArrayList<Frame> successfulFrames;
	private ArrayList<Frame> bufferFrames; 
	int nextFrameIndex;
	private boolean running = false;
	private boolean nack = false;
	private int nackIndex;


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
			running = true;
		} catch (SocketException se) {
			System.out.println("Socket Exception when connecting to client");
		}
		catch (SocketTimeoutException ste) {
			System.out.println("Timeout occured while connecting to client");
		} catch (IOException io) {
			System.out.println("IO exception when connecting to client");
		}
	}


	// Listens for frames from client
	public void listen(){
		try{
			DataInputStream dataInputStream = new DataInputStream(server.getInputStream());
			ObjectInputStream objectInputStream = new ObjectInputStream(dataInputStream);
			while(running){
				try{
					server.setSoTimeout(LISTENER_TIMEOUT_TIME);
					Frame frame =(Frame)objectInputStream.readObject();
					System.out.println("LISTENER: Received frame " + frame.getSequenceNumber() + ": " + frame.getData());
					System.out.println("LISTENER: Checking CRC");
					System.out.print("LISTENER: ");
					if(frame.checkCRC()){
						bufferFrames.add(frame);
						nack = false;
					} else {
						nack = true;
						nackIndex = frame.getSequenceNumber();
					}
					System.out.println();
				} catch (ClassNotFoundException e) {
					System.out.println("Class not found while unpacking frame");
				} 
			}
		}catch (SocketTimeoutException sto){
			// No data on input stream within timeout period: Shut Down
			System.out.println("Socket Timed after " + LISTENER_TIMEOUT_TIME/1000 + "s");
			running = false;
			return;
		}catch (IOException e) {
			System.out.println("Error Creating Input Streams on listener thread");
			e.printStackTrace();
		}
	}

	

	// Thread for transmitting frames to client
	@Override
	public void run() {
		try{
			DataOutputStream dataOutputStream = new DataOutputStream(server.getOutputStream());
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(dataOutputStream);

			while(running){
				if(nack){
					byte[] data = {'n'};
					Frame frame = new Frame((short)nackIndex, (short)1, data);
					System.out.println("TRANSMITTER: Sending NACK for frame " + nackIndex);
					objectOutputStream.writeObject(frame);
				} else if((bufferFrames.size() % WINDOW_SIZE)== 0 && bufferFrames.size() >0){
					// Send ACK
					byte[] data = {'a'};
					short lastSequenceNumber = (short)bufferFrames.get(bufferFrames.size()-1).getSequenceNumber();
					Frame frame = new Frame(lastSequenceNumber, (short)1, data);
					System.out.println("TRANSMITTER: Sending ACK for frames up to frame " + lastSequenceNumber);
					objectOutputStream.writeObject(frame);
					
					// Add buffer frames to success frames and reset buffer
					for(Frame f : bufferFrames){
						successfulFrames.add(f);
					}
					bufferFrames.clear();
				}
				try{
					sleep(TRANSMITER_SLEEP_TIME);
				} catch (InterruptedException e) {
					System.out.println("Transmitter thread interupted while sleeping");
				}
			}

		// Catch Opening Streams Exceptions
		} catch (IOException e) {
			System.out.println("IOException talking to client");
		}

	}


	private void writeToFile(){
		System.out.println("\nData Received");
		// Write to file
		PrintWriter out;
		try {
			out = new PrintWriter("src/output.txt");
			String s = "";
			System.out.println("Received " + successfulFrames.size() + " frames from client");
			for(Frame frame : successfulFrames) {
				System.out.println("Frame " + frame.getSequenceNumber() + ": " + frame.getData());
				s += frame.getData();
			}
			System.out.println(s);
			out.print(s);
			out.close();
		} catch(IOException e) {
			System.out.println(e);
		}
	}


}
