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





public class MyClient extends Thread {
	private static int portNum = 8084;
	private static String serverName = "localhost";

	private static ArrayList<Frame> frames;
	private static final int WINDOW_SIZE = 2;
	private static final int TRANSMITER_SLEEP_TIME = 500;		// Thread Sleep time before checking buffer
	private static final int LISTENER_TIMEOUT_TIME = 5000;		// How long to wait for frame on input stream before shutting down

	private static boolean running = false;

	private static Socket client;

	private static DataInputStream dataInputStream;
	private static DataOutputStream dataOutputStream;

	private static ObjectInputStream objectInputStream;
	private static ObjectOutputStream objectOutputStream;

	private volatile boolean transmitting = false;
	private volatile boolean nackReceived = false;
	private int nackIndex;


	public static void main(String[] args){
		frames = new ArrayList<>();


		MyClient myClient = new MyClient();
		try {
			client = new Socket(serverName, portNum);
			client.setSoTimeout(5000);

			running = true;
			myClient.start();
			myClient.listen();



		} catch (SocketException e) {
			System.out.println("Scoket exception");
		}
		catch (SocketTimeoutException e) {
			System.out.println("Client connection timeout");
			running = false;
			//client.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}


	}


	// Listen for data from server
	public void listen(){
		int sleeps = 0;
		try{
			DataInputStream dataInputStream = new DataInputStream(client.getInputStream());
			ObjectInputStream objectInputStream = new ObjectInputStream(dataInputStream);
			while(running){
				try{

					Frame frame =(Frame)objectInputStream.readObject();

					if(frame.getData().equals("a")){
						System.out.println("ACK received : " + frame.getSequenceNumber());
						//wake writer thread with interupt exception or notify?
						frames.clear();
						nackReceived = false;
					} else if(frame.getData().equals("n")){
						System.out.println("NACK received for " + frame.getSequenceNumber());
						nackReceived = true;
						nackIndex = frame.getSequenceNumber();
						
					}
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
			if(!transmitting){
				return;
			} else {
				listen();
			}

		} catch (SocketTimeoutException e) {
			if(!transmitting){
				return;
			} else {
				listen();
			}
		}

		catch (IOException e) {
			System.out.println("Exception before loop");
			e.printStackTrace();
		}
	}







	// Transmission Thread
	@Override
	public void run(){
		transmitting = true;
		try{
			dataOutputStream = new DataOutputStream(client.getOutputStream());
			objectOutputStream = new ObjectOutputStream(dataOutputStream);

			// Start sending frames
			int c = 0;
			short sequenceNumber = 1;
			FileInputStream in = null;

			try {
				in = new FileInputStream("src/data.txt");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}


			while(c != -1) {

				// Check if we can send next frame
				if(nackReceived){
					Frame frame = null;
					boolean frameFound = false;
					for(Frame f : frames){
						if(f.getSequenceNumber() == nackIndex){
							frame = f;
							frameFound = true;
							break;
						}
					}

					if(!frameFound){
						System.out.println("NACK'd Frame not found in Clients buffer - UhOh");
					} else{
						objectOutputStream.writeObject(frame);
						System.out.println("ReSent Frame " + frame.getSequenceNumber() + " to server");
						System.out.println();
						nackReceived = false;
					}
				}else if(frames.size() < WINDOW_SIZE) {
					byte[] bytes = new byte[8];
					short byteCounter = 0;
					for(int i=0;i<8;i++) {
						try {
							c = in.read();
							if(c == -1) {
								break;
							} 
							bytes[i] = (byte)c;
							byteCounter++;
						} catch (IOException e) {
							System.out.println(e);
						}
					}
					Frame frame = new Frame(sequenceNumber, byteCounter, bytes);
					objectOutputStream.writeObject(frame);
					System.out.println("Sent Frame " + frame.getSequenceNumber() + " to server");
					System.out.println();
					frames.add(frame);
					sequenceNumber++;
				} else {
					try {
						System.out.println("Frame Full (" + frames.size() + ") Sleeping for 500ms");
						sleep(500);
					} catch (InterruptedException e) {
						System.out.println("Thread awoken early : " + e);
					}
				}

			}


			// One finished reading frames: tidy up
			try {
				in.close();
			} catch (IOException e) {
				System.out.println("Error Closing");
			}


			try{
				transmitting = false;
				join();
			} catch (InterruptedException e) {
				System.out.println("Exceiption in waiting for client sender thread to die");
			}
		} catch (IOException e) {
			System.out.println("IOException");
		}

	}








	// Send data to server -- Working full duplex
	/*	@Override
	public void run(){
		transmitting = true;
		try{
			//dataInputStream = new DataInputStream(client.getInputStream());
			dataOutputStream = new DataOutputStream(client.getOutputStream());

			//objectInputStream = new ObjectInputStream(dataInputStream);
			objectOutputStream = new ObjectOutputStream(dataOutputStream);

			for(int i=0;i<10;i++){
				byte[] data = {(byte)(i+63)};
				System.out.println("sending frame");
				objectOutputStream.writeObject(new Frame((short)1, (short)1, data));
				try{
					sleep(1000);
				} catch (InterruptedException e) {
					System.out.println("interupted in dummy sleep");
				}
			}

			// finished transmitting data
			try{
				transmitting = false;
				join();
			} catch (InterruptedException e) {
				System.out.println("Exceiption in waiting for client sender thread to die");
			}
			//client.close();
		} catch (IOException e) {
			System.out.println("IOException");
		}
	}*/


	/*
	static class Sender extends Thread{



		@Override
		public void run() {
			System.out.println("Sender run");
			int c = 0;
			short sequenceNumber = 1;
			FileInputStream in = null;

			try {
				in = new FileInputStream("src/data.txt");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			System.out.println("Running starting : " + c + running);

			while((c != -1) && running) {
				System.out.println("Running");
				// Check if we can send next frame
				if(frames.size() < WINDOW_SIZE) {
					byte[] bytes = new byte[8];
					short byteCounter = 0;
					for(int i=0;i<8;i++) {
						try {
							c = in.read();
							if(c == -1) {
								break;
							} 
							bytes[i] = (byte)c;
							byteCounter++;
						} catch (IOException e) {
							System.out.println(e);
						}
					}
					Frame frame = new Frame(sequenceNumber, byteCounter, bytes);
					sendFrame(frame);
					sequenceNumber++;
				} else {
					try {
						System.out.println("Sleeping for 500ms");
						sleep(500);
					} catch (InterruptedException e) {
						System.out.println("Thread awoken early : " + e);
					}
				}
			}

			// One finished reading frames: tidy up
			try {
				in.close();
			} catch (IOException e) {
				System.out.println("Error Closing");
			}
		}


	 *//**
	 * Sends Frame to the server
	 * @param frame
	 **//*
		private void sendFrame(Frame frame){
			try{
				DataOutputStream dos = new DataOutputStream(client.getOutputStream());
				ObjectOutputStream oos = new ObjectOutputStream(dos);
				System.out.println("Sending frame " + frame.getSequenceNumber()+ ": " +  frame.getData());
				oos.writeObject(frame);
				System.out.println("Frame sent to server");
				frames.add(frame);
			} catch (Exception exception){
				System.out.println("Sending data failed - " + exception);
			}
		}
	}*/

	/*
	static class Receiver extends Thread{
		@Override
		public void run(){
			try{
				DataInputStream dataInputStream = new DataInputStream(client.getInputStream());
				ObjectInputStream objectInputStream = new ObjectInputStream(dataInputStream);
				while(true){
					if(client.isInputShutdown()){
						break;
					}
					try{
						Integer integer = (Integer)objectInputStream.readObject(); 
						System.out.println("Int  = " + integer.intValue());
					} catch (EOFException eofException){
						try{
							System.out.println("Sleeping for 1s");
							sleep(1000);
						} catch (InterruptedException e) {
							System.out.println("Thread awoken on receiver");
							break;
						}
					}
				}
			}catch (IOException e) {
				System.out.println("Io Exception On Receiver");
			} catch (ClassNotFoundException e) {
				System.out.println("Class not found exception on receiver");
			}

		}

		/*		@Override
		public void run() {
			System.out.println("Receiver Run");
			while(running) {
				try {
					DataInputStream dis = new DataInputStream(client.getInputStream());
					ObjectInputStream ois = new ObjectInputStream(dis);
//					objectInputStream = new ObjectInputStream(dataInputStream);
					Frame frame = (Frame) ois.readObject();
					if(frame.getData().equals("a")) {
						System.out.println("Ack received for frame " + frame.getSequenceNumber());
						for(int i=0;i<frames.size();i++) {
							if(frames.get(i).getSequenceNumber() == frame.getSequenceNumber()) {
								frames.remove(i);
								break;
							}
						}
					} else if(frame.getData().equals("n")) {
						System.out.println("Nack received on frame " + frame.getSequenceNumber());
					} else {
						System.out.println("Unknown frame received: " + frame.getData());
					}
				} catch (EOFException eof) {
					try {
						sleep(500);
					} catch (InterruptedException e) {
						System.out.println("Sleep interupt : " + e);
					}
				}

				catch (ClassNotFoundException cnf) {
					System.out.println("Error Unpacking Frame : " + cnf);
				}

				catch (IOException io) {
					System.out.println("Error Unpacking Frame : " + io);
				}
			}
		}
	} */
}