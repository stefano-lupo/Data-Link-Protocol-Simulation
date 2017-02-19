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





public class MyClient {
	private static int portNum = 8084;
	private static String serverName = "localhost";

	private static ArrayList<Frame> frames;
	private static final int WINDOW_SIZE = 2;
	private static boolean running = false;

	private static Socket client;

	private static DataInputStream dataInputStream;
	private static DataOutputStream dataOutputStream;

	private static ObjectInputStream objectInputStream;
	private static ObjectOutputStream objectOutputStream;



	public static void main(String[] args) throws IOException{
		frames = new ArrayList<>();
		Sender sender;
		Receiver receiver;
		try {
			client = new Socket(serverName, portNum);
			client.setSoTimeout(5000);

			dataInputStream = new DataInputStream(client.getInputStream());
			dataOutputStream = new DataOutputStream(client.getOutputStream());

			objectInputStream = new ObjectInputStream(dataInputStream);
			objectOutputStream = new ObjectOutputStream(dataOutputStream);

			sender = new Sender();
			receiver = new Receiver();
			running = true;
			receiver.start();
			sender.start();
		} catch (SocketException e) {

		}
		catch (SocketTimeoutException e) {
			running = false;
			client.close();
		}


	}


	static class Sender extends Thread{

		@Override
		public void run() {
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
				System.out.println(e);
			}
		}


		/**
		 * Sends Frame to the server
		 * @param frame
		 */
		private void sendFrame(Frame frame){
			try{
				System.out.println("Sending frame " + frame.getSequenceNumber()+ ": " +  frame.getData());
				objectOutputStream.writeObject(frame);
				System.out.println("Frame sent to server");
				frames.add(frame);
			} catch (Exception exception){
				System.out.println("Sending data failed - " + exception);
			}
		}
	}

	static class Receiver extends Thread{

		@Override
		public void run() {

			while(running) {
				try {
					Frame frame = (Frame) objectInputStream.readObject();
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
	}
}