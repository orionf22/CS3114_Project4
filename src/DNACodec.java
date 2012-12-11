
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
		implements Codec<DNASequence>
{

	@Override
	public DNASequence decode(byte[] bytes)
	{
		BigInteger string = new BigInteger(bytes);
		return new DNASequence(string.toString(2));
	}

	@Override
	public byte[] encode(DNASequence seq)
	{
		String DNAString = seq.getSequence();
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
		return null;
	}
}
