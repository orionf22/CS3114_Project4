import java.math.BigInteger;

/**
 * This class is a {@link Codec} for storing DNA strings as binary data. It
 * makes use of the {@link BigInteger} class to convert a binary String to bits,
 * then restores those bits. Because leading zeros are lost, cases where A's
 * and/or C's lead are lost. These are restored in
 * {@link Controller#verifyDecode(String, int)}.
 * <p/>
 * @author rinaldi1
 * @author orionf22
 */
public class DNACodec
		implements Codec
{

	@Override
	public String decode(byte[] bytes)
	{
		BigInteger string = new BigInteger(bytes);
		return string.toString(2);
	}

	@Override
	public byte[] encode(String DNAString)
	{
		String binaryString = "";
		for (int i = 0; i < DNAString.length(); i++)
		{
			char curr = DNAString.charAt(i);
			//current char is an "A"; encode as "00"
			if (curr == 65)
			{
				binaryString += "00";
			}
			//curr is a "C"; encode as "01"
			else if (curr == 67)
			{
				binaryString += "01";
			}
			//curr is a "G"; encode as "10"
			else if (curr == 71)
			{
				binaryString += "10";
			}
			//curr is a "T"; encode as "11"
			else if (curr == 84)
			{
				binaryString += "11";
			}
		}
		//this catches any input that has no A's, C's, G's, or T's, as the 
		//length will be 0, which throws a NumberFormatException when 
		//BigInteger's constructor is invoked
		if (binaryString.length() > 0)
		{
			BigInteger bits = new BigInteger(binaryString, 2);
			return bits.toByteArray();
		}
		//nothing in the string pertains to a valid DNA sequence, so return null
		else
		{
			return null;
		}
	}

	/**
	 * Converts a given number to a rounded up value of bytes needed to store a
	 * DNAString. Because each character is represented as two bits, one byte
	 * can store up to four characters. If a DNAString has a length evenly
	 * divisible by four, then the number of bytes needed to store it is equal
	 * to the length of the String divided by four. Otherwise, the number of
	 * bytes needed is the length divided by four, plus one. Any remainder from
	 * dividing by four means additional bits are needed, but not a full byte.
	 * However, a full additional byte is used and extra bits within said byte
	 * are ignored.
	 * <p/>
	 * Examples: <ul>String length = 8; bytes needed = 2
	 * <p/>
	 * String length = 9; bytes needed = 3</ul>
	 * <p/>
	 * @param num the number to always round up
	 * <p/>
	 * @return the rounded up value
	 */
	private static int toByteSize(int num)
	{
		int holder = num / 4;
		double size = (double) num / 4;
		if (size > (double) holder)
		{
			return holder + 1;
		}
		else
		{
			return (int) size;
		}
	}
}
