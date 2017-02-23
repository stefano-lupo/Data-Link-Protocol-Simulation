/*
 * 	Student:		Stefano Lupo
 *  Student No:		14334933
 *  Degree:			JS Computer Engineering
 *  Course: 		3D3 Computer Networks
 *  Date:			21/02/2017
 */


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

/**
 * The Client class is responsible for reading the data to be sent across the network from a text file and packaging it 
 * up in Frame objects. The Client uses the Continuous RQ, Selective Repeat protocol. To accomplish this it transmits up 
 * to WINDOW_SIZE Frames to the server and maintains a buffer of those frames. It will not send any further frames until it
 * receives acknowledgment that the previous frames arrived un-corrupted. On receipt of a negative acknowledgement (nack), 
 * the client retrieves a fresh copy of the corrupted frame from its buffer and retransmits this frame.
 */
public class MyClient extends Thread {
	// Server and Sockets information
	private static int portNum = 8084;
	private static String serverName = "localhost";
	//private static String serverName = "192.168.1.15";		// internal pi address
	private static Socket client;

	/**
	 * The maximum number of frames allowed outstanding between client and server
	 */
	private static final int WINDOW_SIZE = 4;

	/**
	 * Time(ms) for transmitter thread to sleep for after seeing a full buffer.
	 */
	private static final int TRANSMITER_SLEEP_TIME = 100;		

	/**
	 * How long (ms) receiver will wait for a frame from the server before shutting down.
	 */
	private static final int RECEIVER_TIMEOUT_TIME = 4000;	

	/**
	 * Boolean used to communicate between receiver and transmitter thread.
	 */
	private static volatile boolean running = false;

	/**
	 * Buffer containing frames that have been sent to the server but not yet acknowledged.
	 * Maximum length of WINDOW_SIZE
	 */
	private static ArrayList<Frame> frames;

	/**
	 * Boolean that is set by receiver thread (on receipt of a nack) and un-set by transmitter 
	 * (on retransmission of nack'd frame) indicating that a nack has been received, and that 
	 * the relevant frame retransmitted, respectively.
	 */
	private volatile boolean nackReceived = false;

	/**
	 * The sequence number of the missing frame
	 */
	private int nackIndex;


	public static void main(String[] args){

		// Initialize 
		frames = new ArrayList<>();
		MyClient myClient = new MyClient();
		try {
			client = new Socket(serverName, portNum);
			client.setSoTimeout(RECEIVER_TIMEOUT_TIME);
			running = true;

			// Start transmission thread
			myClient.start();

			// Start receiving thread
			myClient.listen();

			// Receiving code has completed at this point
			running = false;

			// Wait for transmission to be finished
			myClient.join();

			// Close connection
			client.close();
		} 

		// Catch required exceptions
		catch(InterruptedException e){
			System.out.println("Main program interupted while waiting for transmission thread to close");
		}

		catch (SocketException e) {
			System.out.println("Socket Exception occured while attempting to connect to server");
		}
		catch (SocketTimeoutException e) {
			System.out.println("Client connection timeout");
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Repeatedly checks the input stream for Frames. Upon receipt of a frame, checks whether frame is an ack
	 * or a nack and communicates to transmitter thread accordingly. Called by main thread.  
	 */
	public void listen(){
		try{
			// Create input streams
			DataInputStream dataInputStream = new DataInputStream(client.getInputStream());
			ObjectInputStream objectInputStream = new ObjectInputStream(dataInputStream);

			// Begin polling
			while(running){
				try{
					//TODO: Ensure no timeout / long timeout is logical
					// 		Need a longer timeout here so that the server can timeout while listening for 
					//		frames from client. This then sends ack for outstanding "odd frames"
					//		which client can then pick up and be disconected by server
					// Reset clock indicating how long to wait for frame
					//client.setSoTimeout(RECEIVER_TIMEOUT_TIME);
//					client.setSoTimeout(1000000);

					// Attempt to read frame from input stream
					Frame frame =(Frame)objectInputStream.readObject();

					// Frame has been read, process it's contents.
					if(frame.getData().equals("a")){
						System.out.println("ACK received : " + frame.getSequenceNumber()+"\n");
						// Ack received - clear buffered frames
						frames.clear();
						nackReceived = false;
					} 

					else if(frame.getData().equals("n")){
						System.out.println("NACK received for " + frame.getSequenceNumber());
						nackReceived = true;
						nackIndex = frame.getSequenceNumber();

					}
				} 

				// Catch Exceptions
				catch (SocketTimeoutException e) {
					System.out.println("Timeout Occured while waiting on ack/nack from server");
					return;
				} 

				catch (ClassNotFoundException e) {
					System.out.println("Error reading frame from input stream - class not found");
				} 

				catch (EOFException eofe){
					System.out.println("Finished Receiving : Connection closed by server");
					return;
				}
			}
		}

		// Catch Opening Streams Exceptions
		catch (IOException e) {
			System.out.println("Error opening input streams on receiving thread");
			e.printStackTrace();
		}
	}







	// Transmission Thread
	@Override
	public void run(){
		try{
			// Create output streams
			DataOutputStream dataOutputStream = new DataOutputStream(client.getOutputStream());
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(dataOutputStream);

			// Start sending frames
			int byteValue = 0;
			short sequenceNumber = 1;
			FileInputStream in = null;

			try {
				in = new FileInputStream("src/data.txt");
			} catch (FileNotFoundException e) {
				System.out.println("Error opening file : data.txt");
			}

			// Begin transmission
			while(running) {

				// Check if a nack has been received (Re-Transmission mode)
				if(nackReceived){
					Frame frame = null;
					boolean frameFound = false;

					// Search for corrupted frame in buffer (by unique sequence number)
					for(Frame f : frames){
						if(f.getSequenceNumber() == nackIndex){
							frame = f;
							System.out.println("Found frame to resend (" + f.getSequenceNumber() + "): " + f.getData());
							frameFound = true;
							break;
						}
					}

					// If frame was not found: serious problem (should never occur)
					if(!frameFound){
						System.out.println("NACK'd Frame not found in Clients buffer - BAD NEWS: " + nackIndex);
						return;
					} 

					/*
					 * ObjectOutputStream maintains a cache of sent objects so sending the same object twice,
					 * (even if modified) results in a non updated version of the object on the remote side.
					 * Resetting the output stream results in retransmitted objects being updated. 
					 * NOTE: 	This is different to flushing the output streams as frames that are on the output
					 * 			stream are NOT removed. 
					 */
					objectOutputStream.reset();	
					objectOutputStream.writeObject(frame);
					System.out.println("Re-sent Frame " + frame.getSequenceNumber() + " : " + frame.getData());
					System.out.println();
					nackReceived = false;

				} 
				
				// No nack received - attempt to transmit more frames
				else if(frames.size() < WINDOW_SIZE && byteValue != -1) {
					// Generate byte array of next 8 characters
					byte[] bytes = new byte[8];
					short byteCounter = 0;
					for(int i=0;i<8;i++) {
						try {
							byteValue = in.read();
							// If end of file is reached, transmit frame
							if(byteValue == -1) {
								break;
							} 
							bytes[i] = (byte)byteValue;
							byteCounter++;
						} 
						// Catch File IO Exception
						catch (IOException e) {
							System.out.println(e);
						}
					}

					/* If above data read the final 8 bytes of the file, the next iteration will reach the 
					 * end of the file and attempt to transmit an invalid frame.
					 * In this case the byte counter will be zero and thus this proble can be filtered out.
					 */
					if(byteCounter != 0) {
						// Must handle the case where the final frame did not contain full 8 characters
						Frame frame = null;
						if(byteCounter != 8) {
							// make a new array of appropriate length
							byte[] nonFullFrameData = new byte[byteCounter];
							
							// fill it with the data
							for(int i=0;i<byteCounter;i++) {
								nonFullFrameData[i] = bytes[i];
							}
							frame = new Frame(sequenceNumber, byteCounter, nonFullFrameData);
						} else {
							frame = new Frame(sequenceNumber, byteCounter, bytes);
						}

						// Send the Frame
						objectOutputStream.writeObject(frame);
						System.out.println("Sent Frame " + frame.getSequenceNumber() + " to server : " + frame.getData()+"\n");
						
						// Add frame to the buffer
						frames.add(frame);
						sequenceNumber++;
					}
				} 
				
				// No nack received and buffer is full
				else {
					try {
						// Check end of file for printing purposes only
						if(byteValue != -1){
							System.out.println("Sleeping for "+TRANSMITER_SLEEP_TIME +"ms as frame buffer is full\n");
						}
						sleep(TRANSMITER_SLEEP_TIME);

					} catch (InterruptedException e) {
						System.out.println("Transmission thread awoken early!");
					}
				}

			}

			// Finished receiving frames - close file.
			try {
				in.close();
			} catch (IOException e) {
				System.out.println("Error Closing");
			}

		} catch (IOException e) {
			System.out.println("IOException");
		}

	}
}