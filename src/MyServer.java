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
			e.printStackTrace();
		}

		// Write result to file
		myServer.writeToFile();

	}


	ServerSocket serverSocket;
	Socket server;
	private static int portNum = 8084;
	private static final int WINDOW_SIZE = 2;
	private static final int TRANSMITER_SLEEP_TIME = 100;		// Thread Sleep time before checking buffer
	private static final int RECEIVER_TIMEOUT_TIME = 5000;		// How long to wait for frame on input stream before shutting down

	private ArrayList<Frame> successfulFrames;
	private ArrayList<Frame> bufferFrames; 
	private ArrayList<Frame> inputStreamCache;
	int nextFrameIndex;
	private volatile boolean running = false;
	private volatile boolean nack = false, checkingNack = false;
	private int nackIndex;


	public MyServer(int port) {
		successfulFrames = new ArrayList<>();
		bufferFrames = new ArrayList<>();
		inputStreamCache = new ArrayList<>();
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
				if(!checkingNack) {
					if(inputStreamCache.isEmpty()){
						try{
							server.setSoTimeout(RECEIVER_TIMEOUT_TIME);
							Frame frame =(Frame)objectInputStream.readObject();
							System.out.println("RECEIVER: Received frame " + nextFrameIndex);
							System.out.println("RECEIVER: Checking CRC");
							System.out.print("RECEIVER: ");
							if(frame.checkCRC()){
								bufferFrames.add(frame);
								nack = false;
								nextFrameIndex ++;
							} else {
								nack = true;
								nackIndex = nextFrameIndex;
								checkingNack = true;		// this is set by other thread otherwise this thread would straight away read the next input
							}
							System.out.println();
						} catch (ClassNotFoundException e) {
							System.out.println("Class not found while unpacking frame");
						} 
					} else {
						Frame frame = inputStreamCache.get(0);
						System.out.println("RECEIVER: Taking cached frame :" + nextFrameIndex);
						System.out.println("RECEIVER: Checking CRC");
						System.out.print("RECEIVER: ");
						if(frame.checkCRC()){
							bufferFrames.add(frame);
							nack = false;
							nextFrameIndex ++;
//							inputStreamClog.remove(0);
						} else {
							nack = true;
							nackIndex = nextFrameIndex;
							checkingNack = true;		// this is set by other thread otherwise this thread would straight away read the next input
						}
						inputStreamCache.remove(0);
						System.out.println();
					}
				} else{
					// Currently checking nack (retransmission mode)
					server.setSoTimeout(RECEIVER_TIMEOUT_TIME);
					// The retransmistted frame will most likely be behind some other frames in the input stream
					// If we flushed the input stream, we would use go back n
					// We need to extract our retransmitted frame, verify it, add it to buffer and then sort through clogged frames

					while(true){
						try{
							Frame frame =(Frame)objectInputStream.readObject();
							if(frame.getSequenceNumber() == nackIndex){
								//TODO: handle CRC on retransmitted correct index frame
								// got to our retransmitted frame
								System.out.println("RECEIVER: Received Re-sent frame " + frame.getSequenceNumber() + " - Note no crc checking here : " +frame.getData());
								System.out.println();
								bufferFrames.add(frame);
								nextFrameIndex++;
								break;
							} else {
								inputStreamCache.add(frame);
							}

						} catch (ClassNotFoundException classNotFoundException){
							System.out.println("Clas not found");
						}
					}

					nack = false; // restart transmitting (ie looking at buffer frames)
					checkingNack = false;
				}
			}
			dataInputStream.close();
			objectInputStream.close();
		}catch (SocketTimeoutException sto){
			// No data on input stream within timeout period: Shut Down
			System.out.println("Listener Socket Timed after " + RECEIVER_TIMEOUT_TIME/1000 + "s");
			if(checkingNack){
				System.out.println("Timeout occured waiting for retransmission of " + nackIndex);
			}
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
					// Send nack
					byte[] data = {'n'};
					Frame frame = new Frame((short)nackIndex, (short)1, data);
					System.out.println("\nTRANSMITTER: Sending NACK for frame " + nackIndex);
					objectOutputStream.writeObject(frame);
					nack = false;
				} else if((bufferFrames.size() % WINDOW_SIZE)== 0 && bufferFrames.size() >0){
					// Send ACK
					byte[] data = {'a'};
					short lastSequenceNumber = (short)bufferFrames.get(bufferFrames.size()-1).getSequenceNumber();
					Frame frame = new Frame(lastSequenceNumber, (short)1, data);
					System.out.println("TRANSMITTER: Sending ACK for frames up to frame " + lastSequenceNumber + "\n");
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

			dataOutputStream.close();
			objectOutputStream.close();
			
			// Catch Opening Streams Exceptions
		} catch (IOException e) {
			System.out.println("IOException talking to client");
		}

	}


	private void writeToFile(){
		if(!bufferFrames.isEmpty()){
			// TODO: Send acks for these frames
			System.out.println("Lingering Buffer Frames being added to successful frames");
			successfulFrames.addAll(bufferFrames);
		}
		System.out.println("\nFinished Receiving " + successfulFrames.size() + " frames from client");
		// Write to file
		PrintWriter out;
		try {
			out = new PrintWriter("src/output.txt");
			String s = "";
			for(Frame frame : successfulFrames) {
				System.out.println("Frame " + frame.getSequenceNumber() + ": " + frame.getData() +" [" + frame.getData().length() + "]");
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
