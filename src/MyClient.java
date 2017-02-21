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
	private static Socket client;

	private static final int WINDOW_SIZE = 8;
	private static final int TRANSMITER_SLEEP_TIME = 10;		// Thread Sleep time before checking buffer
	private static final int RECEIVER_TIMEOUT_TIME = 5000;		// How long to wait for frame on input stream before shutting down

	private static volatile boolean running = false;
	private static ArrayList<Frame> frames;

	private volatile boolean nackReceived = false;
	private int nackIndex;


	public static void main(String[] args){
		frames = new ArrayList<>();


		MyClient myClient = new MyClient();
		try {
			client = new Socket(serverName, portNum);
			client.setSoTimeout(RECEIVER_TIMEOUT_TIME);

			running = true;
			myClient.start();
			myClient.listen();

			// Finished Listening - close connection
			running = false;

		} catch (SocketException e) {
			System.out.println("Scoket exception");
		}
		catch (SocketTimeoutException e) {
			System.out.println("Client connection timeout");
			running = false;
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}


	// Listen for data from server
	public void listen(){
		try{
			DataInputStream dataInputStream = new DataInputStream(client.getInputStream());
			ObjectInputStream objectInputStream = new ObjectInputStream(dataInputStream);
			while(running){
				try{
					client.setSoTimeout(RECEIVER_TIMEOUT_TIME);
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
				} catch (SocketTimeoutException e) {
					System.out.println("Timeout Occured");
					return;
				} catch (ClassNotFoundException e) {
					System.out.println("Class not found in listening");
				} catch (EOFException eofe){
					System.out.println("Finished Listening");
					return;
				}

			}
		}

		// Catch Opening Streams Exceptions
		catch (IOException e) {
			System.out.println("Exception before loop");
			e.printStackTrace();
		}
	}







	// Transmission Thread
	@Override
	public void run(){
		try{
			DataOutputStream dataOutputStream = new DataOutputStream(client.getOutputStream());
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(dataOutputStream);

			// Start sending frames
			int c = 0;
			short sequenceNumber = 1;
			FileInputStream in = null;

			try {
				in = new FileInputStream("src/data.txt");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			while(running) {

				// Check if we can send next frame
				if(nackReceived){
					Frame frame = null;
					boolean frameFound = false;
					for(Frame f : frames){
						if(f.getSequenceNumber() == nackIndex){
							frame = f;
							System.out.println("Found frame to resend (" + f.getSequenceNumber() + "): " + f.getData());
							frameFound = true;
							break;
						}
					}

					if(!frameFound){
						System.out.println("NACK'd Frame not found in Clients buffer - BAD NEWS: " + nackIndex);
					} else{

						//NOTE: 	ObjectOutputStream maintains a cache of sent objects so sending the same object twice (even if modified)
						//			results in a non updated version of the object on the remote side.
						//			After banging my head against the wall I discovered reset() which essentially recreates a fresh object stream
						//			Still a sub-optimal solution
						objectOutputStream.reset();	
						objectOutputStream.writeObject(frame);
						System.out.println("Re-sent Frame " + frame.getSequenceNumber() + " : " + frame.getData());
						System.out.println();
						nackReceived = false;
					}
				} 
				else if(frames.size() < WINDOW_SIZE && c != -1) {
					// Can send next frame
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
						if(c != -1){
							System.out.println("Sleeping for 10ms as frame buffer is full\n");
						}
						sleep(TRANSMITER_SLEEP_TIME);
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

		} catch (IOException e) {
			System.out.println("IOException");
		}

	}
}