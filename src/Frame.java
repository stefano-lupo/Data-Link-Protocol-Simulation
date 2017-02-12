

import java.io.Serializable;
import java.io.ObjectInputStream.GetField;

import javax.annotation.Generated;

public class Frame implements Serializable{
	
	private static final long serialVersionUID = 1L;
//	private static final int PAYLOAD_LENGTH = 8;	// 8 bytes of input data per frame
//	private static final int GENERATOR = 47933;		// 0xBB3D
//	private static final int GENERATOR_LENGTH = 5;
	
	// Frame Contents
	//private byte sequenceNo;			// Sequence number: 1 byte [0,255]
	//private byte[] data;				// 8 bytes of input data
	
	
	private short sequenceNumber;
	private short payloadLength;
	private long data;
	private short remainder;

	
	public Frame(String textfile) {
		// Read from text file containing data
		this.sequenceNumber = 1;
		this.payloadLength = 8;
		this.data = 1234567891234567890L;
		this.remainder = CRC.performCRC(sequenceNumber, payloadLength, data);
	}
	

	
	public boolean checkCRC() {
		return CRC.checkCRC(this.getFullBinary());
	}
	
	public String getSequenceNumberBinary() {
		return CRC.shortToBinary(sequenceNumber);
	}
	
	public String getPayloadLengthBinary() {
		return CRC.shortToBinary(payloadLength);
	}
	
	public long getData(){
		return data;
	}
	
	public String getDataBinary() {
		return CRC.longToBinary(data);
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
	
	
}
