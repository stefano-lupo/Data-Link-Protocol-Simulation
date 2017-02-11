

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class MyServer extends Thread{
	private ServerSocket serverSocket;
	private static int portNum = 8084;
	
	private Frame frame;
	

	public MyServer(int port) throws IOException {
		System.out.println("-------------------------Server--------------------------------");
		serverSocket = new ServerSocket(portNum);
//		serverSocket.setSoTimeout();
	}


	public void run() {
		while(true) {
			try{
				System.out.println("Waiting for client on port " + serverSocket.getLocalPort() + "..");
				Socket server = serverSocket.accept();
				System.out.println("Just connected to " + server.getRemoteSocketAddress());
				
				// Create Data input stream
				DataInputStream dataInputStream = new DataInputStream(server.getInputStream());
				
				// Create object input stream from this data stream
				ObjectInputStream objectInputStream = new ObjectInputStream(dataInputStream);
				
				// Try accessing the object
				try {
					frame = (Frame)objectInputStream.readObject();
					frame.checkCRC();
					
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
				
				// Send receipt message back
				DataOutputStream dataOutputStream = new DataOutputStream(server.getOutputStream());
				dataOutputStream.writeUTF("Connection finishing");
				server.close();

			} catch (SocketTimeoutException s){
				System.out.println("Socket timeout");
				break;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		try {
			Thread t = new MyServer(8083);
			t.start();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}


