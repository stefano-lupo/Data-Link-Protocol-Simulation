

public class CRC {
	private final static int GENERATOR_LENGTH = 16;
	private final static int GENERATOR = 47933;				//0xBB3D
	
	private final static String intLengthFormatString = "%32s"
			, shortLengthFormatString = "%16s"
			, longLengthFormatString = "%64s";
	
	/** 
	 * Computes remainder using CRC 16 Generator polynomial
	 * @param sequenceNumber Sequence number of frame relative to overall data being sent
	 * @param payloadLength Length of data (bytes)
	 * @param data Data to be sent in this frame
	 * @return FCS
	 */
	public static Short performCRC(short sequenceNumber, short payloadLength, long data) {
		// Generate Strings of binary from data
		String sequenceNumberString = String.format(shortLengthFormatString, Integer.toBinaryString(sequenceNumber)).replace(" ", "0");
		String payloadLengthString = String.format(shortLengthFormatString, Integer.toBinaryString(payloadLength)).replace(" ", "0");
		String dataString = String.format(longLengthFormatString, Long.toBinaryString(data)).replace(" ", "0");
		String full = sequenceNumberString + payloadLengthString + dataString;
		
		// Create Generator binary string and substring of generator binary to be used with XOR (ignores 1st digit)
		String generatorString = String.format(shortLengthFormatString, Integer.toBinaryString(GENERATOR)).replace(" ", "0");
		String genSubStr = generatorString.substring(1,generatorString.length());
		
		// append 'generator length' 0s to data to be checked
		for(int i =0;i<GENERATOR_LENGTH;i++) {
			full+=0;
		}
		
		// end of moving substring starts at index = generator length
		int endSubstrIndex = GENERATOR_LENGTH;
		int endOfFull = full.length();
		String substr = full.substring(0,endSubstrIndex);
		endSubstrIndex++;
	
		while(endSubstrIndex < endOfFull) {	
			if(substr.charAt(0) == '1') {
				substr = bitwiseXOR(genSubStr,substr.substring(1));
			} else {
				// XOR with zeros yields itself
				substr = substr.substring(1,GENERATOR_LENGTH);
			}
			// bring down next digit
			substr += full.charAt(endSubstrIndex);
			endSubstrIndex++;			
		}
		
		// If generator contains leading 1: Short.parseInt(substr) complains as number becomes to large 
		// even though it should just be negative
		// Workaround
		return (short)Integer.parseInt(substr,2);
	}

	/**
	 * Performs bitwise a XOR b on two strings of equal length
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
	 * 
	 * @param s
	 * @return
	 */
	public static boolean checkCRC(String s) {
		//System.out.println("Checking CRC on String \t" + s);		
		String generatorString = String.format(shortLengthFormatString, Integer.toBinaryString(GENERATOR)).replace(" ", "0");
		String genSubStr = generatorString.substring(1,generatorString.length());
		
		// Start computation of crc
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
		
		if(substr.contains("1")) {
			System.out.println("ERROR: CRC produced remainder = " + substr);
			return false;
		} else {
			System.out.println("PASS: CRC found no remainder");
			return true;
		}
	}
	

	public static String shortToBinary(short s) {
		String string = String.format(shortLengthFormatString, Integer.toBinaryString(s)).replace(" ", "0");
		if(string.length() > 16){
			return string.substring(16);
		}
		return string;
	}
	
	public static String longToBinary(long d) {
		return String.format(longLengthFormatString, Long.toBinaryString(d)).replace(" ", "0");
	}
	

	

}

