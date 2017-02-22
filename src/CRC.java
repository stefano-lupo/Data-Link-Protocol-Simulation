/*
 * 	Student:		Stefano Lupo
 *  Student No:		14334933
 *  Degree:			JS Computer Engineering
 *  Course: 		3D3 Computer Networks
 *  Date:			21/02/2017
 */


/**
 * 	The CRC class is responsible for examining a Frame object and performing a CRC-16 algorithm to 
 *  compute the checksum that must be placed in the trailer of the frame. It is also responsible for
 *  examining a Frame object which has already had it's checksum calculated and verifying that the data
 *  has or hasn't been corrupted.
 */

public class CRC {
	private final static int GENERATOR = 47933;				//0xBB3D Polynomial
	private final static int GENERATOR_LENGTH = 16;
	
	
	/*
	 * Formating Strings for the conversion of primitive data types to their binary equivalents.
	 */
	private final static String shortLengthFormatString = "%16s"
			, longLengthFormatString = "%64s"
			, byteLengthFormatString = "%8s";
	
	/** 
	 * Computes remainder using CRC 16 Generator polynomial
	 * @param sequenceNumber Sequence number of frame relative to overall data being sent
	 * @param payloadLength Length of data (bytes)
	 * @param data Data to be sent in this frame
	 * @return FCS
	 */
	public static Short performCRC(short sequenceNumber, short payloadLength, byte[] data) {
		// Generate binary String representations of all parts of the frame
		String sequenceNumberString = String.format(shortLengthFormatString, Integer.toBinaryString(sequenceNumber)).replace(" ", "0");
		String payloadLengthString = String.format(shortLengthFormatString, Integer.toBinaryString(payloadLength)).replace(" ", "0");
		String dataString = "";
		for(int i=0;i<data.length;i++){
			dataString+= String.format(byteLengthFormatString, Integer.toBinaryString(data[i])).replace(" ", "0");
		}

		String full = sequenceNumberString + payloadLengthString + dataString;
		
		// Create Generator binary string 
		String generatorString = String.format(shortLengthFormatString, Integer.toBinaryString(GENERATOR)).replace(" ", "0");
		// Create substring of generator binary to be used with XOR as it ignores the 1st digit
		String genSubStr = generatorString.substring(1,generatorString.length());
		
		// append zeros to data string as per CRC-16 Algorithm
		for(int i =0;i<GENERATOR_LENGTH;i++) {		
			full+=0;
		}
		
		// Create pointer to end of sub string to be generated after each iteration
		int endSubstrIndex = GENERATOR_LENGTH;
		// Get length of full string with zeros appended
		int endOfFull = full.length();
		
		// Create first sub string
		String substr = full.substring(0,endSubstrIndex);
		endSubstrIndex++;
	
		// While another substring can be created
		while(endSubstrIndex < endOfFull) {	
			
			// Check if MSB of substring is set (as per CRC-16)
			if(substr.charAt(0) == '1') {
				// perform bitwise XOR if it is
				substr = bitwiseXOR(genSubStr,substr.substring(1));
			} else {
				// Generate new substring (A ^ 0 = A so no need to XOR)
				substr = substr.substring(1,GENERATOR_LENGTH);
			}
			// bring down next digit
			substr += full.charAt(endSubstrIndex);
			endSubstrIndex++;			
		}
		
		// Get 16 bit equivalent of binary remainder
		return (short)Integer.parseInt(substr,2);
	}

	/**
	 * Performs bitwise a XOR b on two strings of equal length
	 * Returns null if strings are not equal length
	 * @param a String of bits 1
	 * @param b String of bits 2
	 * @return XOR'd bits in string form
	 */
	private static String bitwiseXOR(String a, String b) {
		String returnStr = "";
		if(a.length() != b.length()) {
			System.out.println("String length missmatch");
			return null;
		}
		for(int i=0;i<a.length();i++) {
			if(a.charAt(i) == b.charAt(i)) {
				returnStr += 0;
			} else {
				returnStr += 1;
			}
		}
		return returnStr;
	}
	
	
	/**
	 * Checks the Check Sum of a frame which has had its CRC-16 remainder appended to it
	 * @param s binary string representation of frame
	 * @return true if frame is valid, false if frame is corrupted
	 */
	public static boolean checkCRC(String s) {
		// Create generator polynomial binary string and substring for XOR operation
		String generatorString = String.format(shortLengthFormatString, Integer.toBinaryString(GENERATOR)).replace(" ", "0");
		String genSubStr = generatorString.substring(1,generatorString.length());
		
		// Start computation of CRC-16 as in performCRC()
		int endSubstrIndex = GENERATOR_LENGTH;
		int endOfFull = s.length();
		String substr = s.substring(0,endSubstrIndex);
		endSubstrIndex++;
		
		while(endSubstrIndex < endOfFull) {	
			if(substr.charAt(0) == '1') {
				substr = bitwiseXOR(genSubStr,substr.substring(1));

			} else {
				substr = substr.substring(1,GENERATOR_LENGTH);
			}
			substr += s.charAt(endSubstrIndex);
			endSubstrIndex++;
		}
		
		// If any of the bits in the remainder binary are set - frame is corrupt
		if(substr.contains("1")) {
			System.out.println("ERROR - CRC produced remainder = " + substr);
			return false;
		} else {
			System.out.println("PASS - CRC found no remainder");
			return true;
		}
	}
	

	/**
	 * Gets binary String representation of 16 bit number with padding zeros prepended
	 * @param s 16 bit value
	 * @return binary string of length 16
	 */
	public static String shortToBinary(short s) {
		// Must use java's parse 32 bit quantity to avoid overflow problems
		String string = String.format(shortLengthFormatString, Integer.toBinaryString(s)).replace(" ", "0");
		// Ignore first 16 zeros if present
		if(string.length() > 16){
			return string.substring(16);
		}
		return string;
	}
	
	/**
	 * Gets binary String representation of 32 bit number with padding zeros prepended
	 * @param l 32 bit value
	 * @return binary string of length 32
	 */
	public static String longToBinary(long l) {
		return String.format(longLengthFormatString, Long.toBinaryString(l)).replace(" ", "0");
	}
	
	/**
	 * Creates binary String representation of an array of bytes
	 * @param bytes
	 * @return binary String representation of array of bytes
	 */
	public static String byteArrayToBinary(byte[] bytes){
		String dataString = "";
		for(int i=0;i<bytes.length;i++){
			dataString+= String.format(byteLengthFormatString, Integer.toBinaryString(bytes[i])).replace(" ", "0");
		}
		return dataString;
	}

}

