import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

import javax.xml.crypto.Data;

public class MyServer extends Thread{




	public static void main(String[] args) {
		MyServer myServer = new MyServer(8083);
		myServer.start();
		myServer.listen();
		
		
		// Once finished listening: Tidy up
		try{
			System.out.println("Closing Down");
			myServer.server.close();
			myServer.serverSocket.close();
		} catch (Exception e) {
			System.out.println("Exception closing sockets");
		}
	}





	ServerSocket serverSocket;
	Socket server;
	private static int portNum = 8084;

	private ArrayList<Frame> successfulFrames;
	private ArrayList<Frame> bufferFrames; 
	int nextFrameIndex;
	private Frame frame;
	private boolean running = false;

	//	private DataInputStream dataInputStream;
	//	private DataOutputStream dataOutputStream;
	//	private ObjectInputStream objectInputStream;
	//	private ObjectOutputStream objectOutputStream;





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
			//			dataInputStream = new DataInputStream(server.getInputStream());
			//			dataOutputStream = new DataOutputStream(server.getOutputStream());
			//			System.out.println(dataInputStream.readInt());
			//			objectInputStream = new ObjectInputStream(dataInputStream);



			//			try{
			//				Frame frame = (Frame)objectInputStream.readObject();
			//				System.out.println(frame.getData());
			//			} catch (ClassNotFoundException classNotFoundException){
			//				System.out.println("classnotfoujs");
			//			}

			//server.close();
			//dataOutputStream = new DataOutputStream(server.getOutputStream());

			// Create Object I/O Streams
			//objectInputStream = new ObjectInputStream(dataInputStream);
			//objectInputStream.readObject();
			//objectOutputStream = new ObjectOutputStream(dataOutputStream);



			running = true;
		} catch (SocketException se) {
			se.printStackTrace();
		}
		catch (SocketTimeoutException ste) {
			System.out.println("Timedout");
			running = false;
		} catch (IOException io) {
			//io.printStackTrace();
		}
	}


	public void listen(){
		int sleeps = 0;
		try{
			DataInputStream dataInputStream = new DataInputStream(server.getInputStream());
			ObjectInputStream objectInputStream = new ObjectInputStream(dataInputStream);
			while(running){
				try{
					Frame frame =(Frame)objectInputStream.readObject();
					System.out.println("Received frame : " + frame.getSequenceNumber());
					sleeps = 0;
				} catch (ClassNotFoundException e) {
					System.out.println("Class not found in listening");
				} catch (EOFException eofe){
					try{
						System.out.println("Sleepin");
						sleep(1000);
						sleeps++;
						if(sleeps >= 5){
							break;
						}
					} catch (InterruptedException e) {
						System.out.println("Interupted");
					}
				}
				
			}
		
		} catch (IOException e) {
			System.out.println("Exception before loop");
			e.printStackTrace();
		}
	}

	
	// Thread for talking to client
	@Override
	public void run() {
		System.out.println("Second thread running");
		try{
			DataOutputStream dataOutputStream = new DataOutputStream(server.getOutputStream());
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(dataOutputStream);
			for(int i=0;i<10;i++){
				byte[] data = {(byte)(63+i)};
				Frame frame = new Frame((short)1, (short)1, data);
				System.out.println("sending frame :" + frame.getData());
				objectOutputStream.writeObject(frame);
				try{
					sleep(500);
				} catch (InterruptedException e) {
					System.out.println("interupt in dummy sleep");
				}
			}

		} catch (IOException e) {
			System.out.println("IOException talking to client");
		}

		/*		try{
			DataOutputStream dataOutputStream = new DataOutputStream(server.getOutputStream());
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(dataOutputStream);
			for(int i=0;i<3;i++){
				objectOutputStream.writeObject(new Integer(99));
				try{
					sleep(2000);
				} catch (InterruptedException e){
					System.out.println("Server sleep interupted");
				}
			}
		server.close();
		serverSocket.close();
		} catch (IOException e) {
			System.out.println("Io exception on server");
		}*/




		//		while(running) {
		//			System.out.println("Starting");
		//			// Welcome client to Server
		//			//dataOutputStream.writeUTF("SERVER: You have succesfully connected");
		//			//
		//			//				// Create object input stream from this data stream
		//			//				ObjectInputStream objectInputStream = new ObjectInputStream(dataInputStream);
		//
		//			// Try accessing the object
		//			try {
		//				DataInputStream dis = new DataInputStream(server.getInputStream());
		//				ObjectInputStream ois = new ObjectInputStream(dis);
		//				frame = (Frame)ois.readObject();
		//				System.out.println("Frame Received from client - Checking CRC");
		//				byte[] ack = new byte[1];
		//				if(frame.checkCRC()){
		//					successfulFrames.add(frame);
		//					nextFrameIndex++;
		//					ack[0] = 'a';
		//				} else {
		//					successfulFrames.add(frame);
		//					ack[0] = 'n';
		//				}
		//
		//				// Return the ack
		//				Frame returnFrame = new Frame(frame.getSequenceNumber(),(short)1, ack);
		//				DataOutputStream dos = new DataOutputStream(server.getOutputStream());
		//				ObjectOutputStream oos = new ObjectOutputStream(dos);
		//				oos.writeObject(returnFrame);
		//
		//			} catch (ClassNotFoundException e) {
		//				e.printStackTrace();
		//			} catch (IOException io) {
		//				System.out.println("IO exception");
		//				io.printStackTrace();
		//			}
		//		}

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




}
