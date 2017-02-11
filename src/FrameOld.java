

import java.io.Serializable;

public class FrameOld implements Serializable{

	private static final long serialVersionUID = 1L;
	private static final int PAYLOAD_LENGTH = 8;	// 8 bytes of input data per frame
	private static final int GENERATOR = 47933;		// 0xBB3D
	private static final int GENERATOR_LENGTH = 5;

	// Frame Contents
	private byte sequenceNo;			// Sequence number: 1 byte [0,255]
	private byte[] data;				// 8 bytes of input data
	private long fcs;					// 
	private long valueOfData = 12345678;


	FrameOld(byte[] data){ 
		sequenceNo = 0;
		this.data = data;
	}



	public byte[] getData() {
		return data;
	}

	public void CRC() {

//		System.out.println("Ans = " + (3 ^ 5));
//		valueOfData = 0;
//		for(int i=0;i<PAYLOAD_LENGTH;i++) {
//			valueOfData+= data[i];
//			if(i < PAYLOAD_LENGTH - 1) {
//				valueOfData = valueOfData *10;
//			}
//			System.out.println("val = " + valueOfData);
//		}
//		System.out.println("valueOfData = " + valueOfData);
//		fcs = GENERATOR - (valueOfData % GENERATOR);
//		System.out.println("fcs = " + fcs);
		
		long tempdata = valueOfData;
		long tempgenerator = 0;
		int numDigits = 0;
		int counter = 1;
		
		
		while(tempdata >= 1) {
			numDigits++;
			tempdata /= 10;
		}
		System.out.println("Num digits = " + numDigits);
		
		tempdata = valueOfData;
	
		
		// old way
//		while(tempdata >= x) {
//		// gets first generator length digits
//		tempdata /= 10;
//		counter++;
//	}
		
		long x = (long)Math.pow(10, numDigits - GENERATOR_LENGTH);
		long firstGenDigits = tempdata / x;						//yields 12345

		System.out.println("First gen digits = " + firstGenDigits );
		
		
		if(firstGenDigits >= GENERATOR) {
			// Need to get last (Generator - 1) digits
			firstGenDigits = (long)(firstGenDigits % Math.pow(10, GENERATOR_LENGTH - 1));
			tempgenerator = (long)(GENERATOR %  Math.pow(10, GENERATOR_LENGTH - 1));
			System.out.println("firstGenDigits = " + firstGenDigits + ", tempgenerator = " + tempgenerator);
			firstGenDigits = firstGenDigits ^ GENERATOR;
		} else {
			// Need to get last four digits (Redundant !!)
			firstGenDigits = (long)(firstGenDigits % Math.pow(10, GENERATOR_LENGTH - 1));
			tempgenerator = (long)(GENERATOR %  Math.pow(10, GENERATOR_LENGTH - 1));
			System.out.println("firstGenDigits = " + firstGenDigits + ", tempgenerator = " + tempgenerator);
			firstGenDigits = firstGenDigits ^ 0;
		}
		
		//From here der
//		tempdata = valueOfData / (numDigits - (GENERATOR_LENGTH + counter)) % 10;
//		System.out.println("tempdata = " + tempdata);
	
	}

	public long getFcs() {
		return fcs;
	}

	public int getGenerator() {
		return GENERATOR;
	}

	public long getValueOfData() {
		return valueOfData;
	}
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		for(int i=0;i<data.length;i++) {
			stringBuilder.append(data[i] + ",");
		}
		System.out.println("SeqNo = " + sequenceNo + ", payloadLength = " + PAYLOAD_LENGTH +
				", data = " + stringBuilder.toString() + " FCS = " + fcs);
		return stringBuilder.toString();
	}
}


