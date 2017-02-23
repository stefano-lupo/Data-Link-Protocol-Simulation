/*
 * 	Student:		Stefano Lupo
 *  Student No:		14334933
 *  Degree:			JS Computer Engineering
 *  Course: 		3D3 Computer Networks
 *  Date:			21/02/2017
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * The Server Class is responsible for opening a ServerSocket and hosting a client connection. The Server is contains a 
 * thread for transmitting ACK/NACK frames to the client and a thread for listening for data frames from the client. 
 * Valid frames received by the server are stored in an ArrayList called bufferFrames. Once this array list reaches the 
 * designated window size (given by WINDOW_SIZE), the server will then send an acknowledgment frame to the client, 
 * indicating that it can now receive more frames as once this point is reached, the contents of bufferFrames are added to
 * the successfulFrames ArrayList and bufferedFrames is cleared. 
 * 
 * Upon receipt of a frame the server will evaluate its checksum and detect whether corruption has occurred using the 
 * CRC-16 algorithm. If corruption has occurred, it will enter re-transmission mode. 
 * Once retransmission mode has been entered, the server sends a nack to the client requesting them to re-send 
 * the missing frame. The server will then cache any outstanding frames from the client and accept the resent frame. 
 * This frame is then re-checked and if it is invalid another retransmission is requested (and so on). 
 * If it is valid, the server accepts this frame and then proceeds to check the frames that were cached earlier. 
 * This is the mechanism that allows the server to operate in Continuous RQ, Selective Repeat. 
 * When an invalid frame occurs, the server caches any outstanding frames and thus only requires retransmission 
 * of that specific frame. 
 *
 */
public class MyServer extends Thread{


	public static void main(String[] args) {
		MyServer myServer = new MyServer(8084);

		// Start transmission thread
		myServer.start();			
		
		// Start receiver thread
		myServer.listen();									

		// Server finished receiving, wait until transmission thread is also finished
		// (sending final ack for "uneven" window count)
		try{
			myServer.join();
		} 
		catch (InterruptedException e) {
			System.out.println("Interuption while waiting for transmitter to send final ack");
		}

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

		// Test results are identical
		myServer.checkResults();
	}

	// Server and Socket INFO
	ServerSocket serverSocket;
	Socket server;
	private static final int WINDOW_SIZE = 5;
	
	/**
	 * Time(ms) for transmitter thread to sleep for after seeing a NON full buffer.
	 */
	private static final int TRANSMITER_SLEEP_TIME = 50;		

	/**
	 * How long (ms) receiver will wait for a frame from the client before shutting down.
	 */
	private static final int RECEIVER_TIMEOUT_TIME = 1000;	
	
	/**
	 * Holds in order and verified frames from client.
	 * Valid frames from bufferedFrames are added to this list when the window size is reached 
	 * and an ACK is sent
	 */
	private ArrayList<Frame> successfulFrames;
	
	/**
	 * Holds frames with valid checksum until ACK can be sent.
	 * Once ACK can be sent, these frames are transferred to successfulFrames and this
	 * list is cleared for the receipt of more frames.
	 */
	private ArrayList<Frame> bufferFrames; 
	
	/**
	 * When an invalid frame arrives, a NACK is issued to the client. Any outstanding frames on the input stream
	 * must be cached so that the input stream can be cleared to receive the corrupted frame that will be resent 
	 * by the client. These outstanding frames are cached here. 
	 * These frames are then processed in order once the previous corrupted frame has been received.
	 */
	private ArrayList<Frame> inputStreamCache;
	
	/**
	 * Holds the next frames expected sequence number (initially 1)
	 */
	int nextFrameIndex = 1;
	
	/**
	 * A shared boolean used to cause execution of both threads to end when necessary
	 */
	private volatile boolean running = false;
	
	/**
	 * A shared boolean which is set by the receiver on receipt of a corrupted frame. 
	 * This boolean is examined by the transmitter thread, allowing it to know whether it should transmit
	 * an ACK or a NACK.
	 */
	private volatile boolean nack = false;
	
	/**
	 * A shared boolean which indicates that the current frame being checked on the receiver thread is one
	 * that was re-transmitted as a result of corruption. This results in a frame being read from the cachedFrames
	 * list as opposed to the bufferedFrames list
	 */
	private volatile boolean checkingNack = false;
	
	/**
	 * Holds the expected sequence number of the frame to be re-transmitted
	 */
	private int nackIndex;

	
	public MyServer(int port) {
		// Initialize lists
		successfulFrames = new ArrayList<>();
		bufferFrames = new ArrayList<>();
		inputStreamCache = new ArrayList<>();
		System.out.println("-------------------------Server--------------------------------");

		try {
			// Open the Socket
			serverSocket = new ServerSocket(port);
			
			// Wait for timeout time for client to connect (optional)
			serverSocket.setSoTimeout(10000);
			System.out.println("Waiting for client on port " + serverSocket.getLocalPort() + "..");
			server = serverSocket.accept();
			System.out.println("Just connected to " + server.getRemoteSocketAddress() + "\n");
			running = true;
		} 
		
		// Catch exceptions associated with opening socket
		catch (SocketException se) {
			System.out.println("Socket Exception when connecting to client");
			se.printStackTrace();
		}
		catch (SocketTimeoutException ste) {
			System.out.println("Timeout occured while connecting to client");
		} 
		catch (IOException io) {
			System.out.println("IO exception when connecting to client");
		}
	}


	/**
	 * Receives Frames from client
	 */
	public void listen(){
		try{
			// Create input streams
			DataInputStream dataInputStream = new DataInputStream(server.getInputStream());
			ObjectInputStream objectInputStream = new ObjectInputStream(dataInputStream);
			
			// Poll the input streams
			while(running){
			
				if(!checkingNack) {
					if(inputStreamCache.isEmpty()){
						// No cached frames - accept next frame on input stream
						try{
							// Set allowable time between frames
							server.setSoTimeout(RECEIVER_TIMEOUT_TIME);
							
							// Get frame from input stream
							Frame frame =(Frame)objectInputStream.readObject();
							System.out.println("RECEIVER: Received frame " + nextFrameIndex);
							System.out.println("RECEIVER: Checking CRC");
							System.out.print("RECEIVER: ");
							if(frame.checkCRC()){
								// Frame is valid, add to buffer
								bufferFrames.add(frame);
								nack = false;
								nextFrameIndex ++;
							} else {
								// Frame invalid, tell transmitter to request retransmission
								nack = true;
								nackIndex = nextFrameIndex;
								// Tell ourselves not to take any more frames until NACK is handled
								// This boolean is un-set once corrupted frame has been fixed
								checkingNack = true;
							}
							System.out.println();
						} 
						
						// Catch unpacking exception
						catch (ClassNotFoundException e) {
							System.out.println("ERROR: Invalid Frame - Class not found while unpacking frame");
						} 
					} 
					
					// NOT checking NACK but have cached frames remaining
					else {
						// Get next cached frame
						Frame frame = inputStreamCache.get(0);
						System.out.println("RECEIVER: Taking cached frame " + nextFrameIndex + ": " + frame.getData());
						System.out.println("RECEIVER: Checking CRC");
						System.out.print("RECEIVER: ");
						// Validate Frame
						if(frame.checkCRC()){
							bufferFrames.add(frame);
							nack = false;
							nextFrameIndex ++;
						} else {
							nack = true;
							nackIndex = nextFrameIndex;
							checkingNack = true;
						}
						// remove cached frame either way (replaced if NACK, saved if ACK)
						inputStreamCache.remove(0);
						System.out.println();
					}
				} 
				
				// END NOT CHECKING NACK
				
				
				// ELSE CHECKING NACK
				else {
					
					// Nack Handling
					
					// The re-transmitted frame will most likely be behind some other frames in the input stream
					// We must cache these frames to avoid having to re-transmit those also (go back n)
					// We need to find our retransmitted frame and add it to cachedFrames[0] so it will be checked first 
					// and consume the NACK
					
					
					// The last frame on the input stream will always be the retransmitted frame
					// Need to get the last frame from the input stream, cache the others and put the last frame first

					// Max Number of frames on the input stream is window size
					// This is because although we are retransmitting another frame, that same corrupted frame was 
					// previously read from the input stream to start the nack process and thus is no longer there
					// Read from input stream a max of WINDOW_SIZE times or until a timeout occurs
					// A timeout is required as there may not be a full "WINDOW_SIZE" Frames on the input stream
					// if the corrupted frame was not the first frame in that window
					
					//wait till transmitter sends nack
					while(nack){}
					
					
					// Calculate how many frames should be in input stream at this point
					int expectedFramesInStream = ((WINDOW_SIZE - ((nextFrameIndex -1)% WINDOW_SIZE))- inputStreamCache.size());
					System.out.println("Expected frames in input stream = " + expectedFramesInStream);
					
	
					
					int x = 1;
					while(x <= expectedFramesInStream){
						// Reset the timeout
						// Timeout is needed as expected number of frames can't know if there is not enough frames 
						// left on client side
						// Eg works out correct number of frames that should be left but the clients last block of frames 
						// weren't of length = window size so there wont be that many in there
						// In that case just latch the last frame we get by using the time out
						// This timeout can be long without being degrading performance as it will only happen at most once
						server.setSoTimeout(200);
					
						try{
							// Create array list in same order as input stream
							// Last item will be our retransmitted frame
							Frame frame = (Frame)objectInputStream.readObject();
							inputStreamCache.add(frame);
							x++;
						}
						catch (ClassNotFoundException e) {
							System.out.println("Class not found exception");
						}
						catch (SocketTimeoutException e) {
							System.out.println("socket timeout occured in new code");
							break;
						}
					}
					
					
					// Get the last frame in the list
					Frame retransmittedFrame = inputStreamCache.get(inputStreamCache.size()-1);
					
					
					// Remove that frame from list
					inputStreamCache.remove(retransmittedFrame);
					
					// Add it back to start of list
					inputStreamCache.add(0,retransmittedFrame);
					
					
					System.out.print("After Reordering frames from input stream: (");
					for(Frame f : inputStreamCache){
						System.out.print(f.getSequenceNumber()+",");
					}				
					System.out.println(")\n");
					
					// Retransmitted frame found - consume NACK
					nack = false;
					checkingNack = false;
				}
			}
			
			// Finished Receiving - close data streams
			dataInputStream.close();
			objectInputStream.close();
		}
		
		// Catch timeout
		catch (SocketTimeoutException sto){
			// No data on input stream within timeout period
			System.out.println("Listener Socket Timed out after " + RECEIVER_TIMEOUT_TIME/1000 + "s");
			if(checkingNack){
				System.out.println("Timeout occured waiting for retransmission of " + nackIndex);
			}
			running = false;
			return;
		}
		
		catch (IOException e) {
			System.out.println("Error Creating Input Streams on listener thread");
			e.printStackTrace();
		}
	}



	/**
	 * Thread for transmitting frames to client
	 */
	@Override
	public void run() {
		try{
			// Initialize output streams
			DataOutputStream dataOutputStream = new DataOutputStream(server.getOutputStream());
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(dataOutputStream);

			while(running){
				
				// Check if NACK is to be transmitted
				if(nack){
					// Send NACK
					byte[] data = {'n'};
					Frame frame = new Frame((short)nackIndex, (short)1, data);
					System.out.println("TRANSMITTER: Sending NACK for frame " + nackIndex);
					objectOutputStream.writeObject(frame);
					nack = false;
				} 
				
				else if((bufferFrames.size() % WINDOW_SIZE)== 0 && bufferFrames.size() >0){
					// Window Size reached, send ACK to client
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
					// Sleep to allow Reciver thread to process frames
					sleep(TRANSMITER_SLEEP_TIME);
				} catch (InterruptedException e) {
					System.out.println("Transmitter thread interupted while sleeping");
				}
			}

			if(!bufferFrames.isEmpty()){
				// Save remaning buffer frames that have not yet been acked due to not reaching windowSize
				System.out.println("Adding remaining " + bufferFrames.size() + " buffer frames to succesful frames");
				successfulFrames.addAll(bufferFrames);
				
				// Send client ACK for these frames also
				byte[] data = {'a'};
				Frame ack = new Frame(bufferFrames.get(bufferFrames.size()-1).getSequenceNumber(), (short)1, data);
				System.out.println("Ack sent for "+ack.getSequenceNumber());
				objectOutputStream.writeObject(ack);
			}
			
			// Close output streams
			dataOutputStream.close();
			objectOutputStream.close();
			
			// Catch Opening Streams Exceptions
		} catch (IOException e) {
			System.out.println("IOException talking to client");
			e.printStackTrace();
		}
	}


	/**
	 * Writes data of all frames contained in successfulFrames ArrayList to src/output.txt
	 */
	private void writeToFile(){
		System.out.println("\nFinished Receiving " + successfulFrames.size() + " frames from client");
		
		// Write to file
		PrintWriter out;
		try {
			out = new PrintWriter("src/output.txt");
			String s = "";
			
			// Iterate over each frame and create string
			for(Frame frame : successfulFrames) {
				System.out.println("Frame " + frame.getSequenceNumber() + ": " + frame.getData() +" [" + frame.getData().length() + "]");
				s += frame.getData();
			}
			System.out.println(s);
			
			// Write and close file
			out.print(s);
			out.close();
		} catch(IOException e) {
			System.out.println(e);
		}
	}
	
	
	/**
	 * Checks if what ends up in output.txt matches exactly with data.txt
	 */
	private void checkResults(){
		String data = null;
		String output = null;
		
		try{
			data = new String(Files.readAllBytes(Paths.get("src/data.txt"))); 
			output = new String(Files.readAllBytes(Paths.get("src/output.txt")));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(data.equals(output)){
			System.out.println("\nData matches exactly");
		} else {
			System.out.println("\nData DOES NOT match");
		}
		
	}

}
