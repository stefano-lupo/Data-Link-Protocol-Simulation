

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
		this.data = 31l;
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
	
	public String getDataBinary() {
		return CRC.longToBinary(data);
	}
	
	public String getRemainderBinary() {
		return CRC.shortToBinary(this.remainder);
	}
	
	public String getOriginalBinary() {
		return getSequenceNumberBinary() + getPayloadLengthBinary() + getDataBinary();
	}
	
	public String getFullBinary() {
		return getSequenceNumberBinary() + getPayloadLengthBinary() + getDataBinary() + getRemainderBinary();
	}
	
	
}
