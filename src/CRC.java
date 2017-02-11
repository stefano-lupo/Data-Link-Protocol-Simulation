

public class CRC {
	private final static int HEADER_LENGTH = 32;
	private final static int DATA_LENGTH = 64;
	
	private final static int GENERATOR_LENGTH = 16;
	private final static int GENERATOR = 47933;				//0xBB3D
	
	private final static String intLengthFormatString = "%32s"
			, shortLengthFormatString = "%16s"
			, longLengthFormatString = "%64s";
	
	public static Short performCRC(short sequenceNumber, short payloadLength, long data) {
//	public static String performCRC(long header, long data) {
//		headerLengthFormatString = "%" + String.valueOf(HEADER_LENGTH) + "s";
//		generatorLengthFormatString = "%" + String.valueOf(GENERATOR_LENGTH) + "s";
//		dataLengthFormatString = "%" + String.valueOf(DATA_LENGTH) + "s";
		
		String sequenceNumberString = String.format(shortLengthFormatString, Integer.toBinaryString(sequenceNumber)).replace(" ", "0");
		String payloadLengthString = String.format(shortLengthFormatString, Integer.toBinaryString(payloadLength)).replace(" ", "0");
		String dataString = String.format(longLengthFormatString, Long.toBinaryString(data)).replace(" ", "0");
		String full = sequenceNumberString + payloadLengthString + dataString;
		
		String generatorString = String.format(shortLengthFormatString, Integer.toBinaryString(GENERATOR)).replace(" ", "0");
		String genSubStr = generatorString.substring(1,generatorString.length());
		
		// append 'generator length' 0s to data to be checked
		for(int i =0;i<GENERATOR_LENGTH;i++) {
			full+=0;
		}
		System.out.println("Full str = " + full);
		int endSubstrIndex = GENERATOR_LENGTH;
		int endOfFull = full.length();
		System.out.println("full length = " + endOfFull);
		String substr = full.substring(0,endSubstrIndex);
		endSubstrIndex++;
	
		while(endSubstrIndex < endOfFull) {	
			if(substr.charAt(0) == '1') {
				substr = bitwiseXOR(genSubStr,substr.substring(1));
			} else {
				// XOR with zeros yields itself
				substr = substr.substring(1,GENERATOR_LENGTH);
			}
			substr += full.charAt(endSubstrIndex);
			endSubstrIndex++;			
		}
		return Short.parseShort(substr, 2);
	}

	
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
	
	
	public static boolean checkCRC(String s) {
		System.out.println("Checking CRC on String \t" + s);
//		String sequenceNumberString = String.format(shortLengthFormatString, Integer.toBinaryString(sequenceNumber)).replace(" ", "0");
//		String payloadLengthString = String.format(shortLengthFormatString, Integer.toBinaryString(payloadLength)).replace(" ", "0");
//		String dataString = String.format(longLengthFormatString, Long.toBinaryString(data)).replace(" ", "0");
//		String full = sequenceNumberString + payloadLengthString + dataString;
		
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
//			System.out.println(substr);
		}
		
		if(substr.contains("1")) {
			System.out.println("Corrupted Data - remainder = " + substr);
			return false;
		} else {
			System.out.println("Data is good");
			return true;
		}
	}
	
	
	public static String shortToBinary(short s) {
		return String.format(shortLengthFormatString, Integer.toBinaryString(s)).replace(" ", "0");
	}
	
	public static String longToBinary(long d) {
		return String.format(longLengthFormatString, Long.toBinaryString(d)).replace(" ", "0");
	}
	
	

}

