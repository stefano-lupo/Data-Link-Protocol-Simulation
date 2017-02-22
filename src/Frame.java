/*
 * 	Student:		Stefano Lupo
 *  Student No:		14334933
 *  Degree:			JS Computer Engineering
 *  Course: 		3D3 Computer Networks
 *  Date:			21/02/2017
 */


import java.io.Serializable;
import java.util.Random;

/**
 * 	The Frame class is responsible for creating a frame from a byte array of data. The Frame class also provides
 *  utility methods for calculating and checking the checksum of the frame using the CRC-16 Algorithm.
 */

public class Frame implements Serializable{
	
	private static final long serialVersionUID = 1L;	
	
	
	private short sequenceNumber;
	private short payloadLength;
	private byte[] data;
	private short remainder;

	
	/**
	 * Creates a Frame and automatically performs the CRC-16 algorithm and adds the appropriate checksum to the trailer
	 * @param sequenceNumber
	 * @param payloadLength
	 * @param data
	 */
	public Frame(short sequenceNumber, short payloadLength, byte[] data) {
		this.sequenceNumber = sequenceNumber;
		this.payloadLength = payloadLength;
		this.data = data;
		this.remainder = CRC.performCRC(this.sequenceNumber, this.payloadLength, this.data);
	}
	

	/**
	 * Computes the checksum of the frame and checks whether or not the frame is valid.
	 * This also calls a gremlin function which will corrupt the frame in some way approximately 50% of the time
	 * @return true if frame is valid, false otherwise.
	 */
	public boolean checkCRC() {
		gremlin();
		return CRC.checkCRC(this.getFullBinary());
	}
	
	/*
	 * Normal Getters/Setters for binary strings
	 */
	public String getSequenceNumberBinary() {
		return CRC.shortToBinary(sequenceNumber);
	}
	
	public String getPayloadLengthBinary() {
		return CRC.shortToBinary(payloadLength);
	}
	
	public String getDataBinary() {
		return CRC.byteArrayToBinary(data);
	}
	
	public String getRemainderBinary() {
		return CRC.shortToBinary(remainder);
	}
	
	/**
	 * @return Sequence Number + PayloadLength  + Data in binary
	 */
	public String getOriginalBinary() {
		return getSequenceNumberBinary() + getPayloadLengthBinary() + getDataBinary();
	}
	
	/**
	 * @return Sequence Number + PayloadLength  + Data + Remainder in binary
	 */
	public String getFullBinary() {
		return getSequenceNumberBinary() + getPayloadLengthBinary() + getDataBinary() + getRemainderBinary();
	}
	
	
	
	
	/*
	 * Normal Getter and Setters
	 */
	public short getSequenceNumber() {
		return sequenceNumber;
	}
	
	public short getPayloadLength() {
		return payloadLength;
	}
	
	public String getData(){
		String string = "";
		for(int i=0;i<data.length;i++){
			string += (char)data[i];
		}
		return string;
	}
	
	public byte[] getDataBytes(){
		return data;
	}
	
	
	
	/**
	 * Randomly corrupts some aspect of the frame on approximately half of all calls to the function.
	 */
	private void gremlin(){
		Random r = new Random();
		int random = r.nextInt(10);
		
		// Half of the time corrupt the frame
		if(random > 5){
			if(random == 7){
				// corrupt payload length
				System.out.println("GREMLIN : Corrupting Payload length on frame " + sequenceNumber);
				payloadLength = (short)r.nextInt(65535);
			} 
			else if(random == 6){
				// corrupt sequence number
				System.out.println("GREMLIN : Corrupting Sequence Number on frame " + sequenceNumber);
				sequenceNumber  = (short)r.nextInt(65535);
			} 

			else if(random == 8){
				System.out.println("GREMLIN : Corrupting Data on frame " + sequenceNumber);
				// always corrupt first byte with star
				data[0] = '*';
				
				// randomly corrupt rest of bytes
				for(int i=1;i<data.length;i++){
					if(r.nextInt(2) == 1){
						data[i] = (byte)r.nextInt(256);
					}
				}
			}
			else if(random == 9){
				System.out.println("GREMLIN : Corrupting Remainder on frame " + sequenceNumber);
				remainder = (short)r.nextInt(65535);
			}
		}
	}
	
}
