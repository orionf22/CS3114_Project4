
/**
 * {@code DNASequence} objects wrap a given String as a DNA sequence. DNA
 * sequences are expected to contain any of A, C, G, or T and no other
 * characters. {@code sequence} maintains the true sequence intended and is
 * never modified; {@code current} maintains a progressive iteration of the
 * original sequence and can be modified for purposes of insertion, removal, and
 * searching.
 * <p/>
 * @author orionf22
 * @author rinaldi1
 */
public class DNASequence
{

	/**
	 * The true sequence.
	 */
	private String sequence;
	/**
	 * The current iteration of the sequence.
	 */
	private String current;
	/**
	 * String A.
	 */
	public static final String BASE_A = "A";
	/**
	 * String C.
	 */
	public static final String BASE_C = "C";
	/**
	 * String G.
	 */
	public static final String BASE_G = "G";
	/**
	 * String T.
	 */
	public static final String BASE_T = "T";
	/**
	 * String $.
	 */
	public static final String TERMINATOR = "$";

	/**
	 * Constructs a new {@code DNASequence} from {@code seq}.
	 * <p/>
	 * @param seq the String sequence to use
	 */
	public DNASequence(String seq)
	{
		this.sequence = seq;
		this.current = seq;
	}

	/**
	 * Returns the true sequence.
	 * <p/>
	 * @return the true sequence
	 */
	public String getSequence()
	{
		return this.sequence;
	}

	/**
	 * Returns the current iteration of the sequence.
	 * <p/>
	 * @return the current iteration of sequence
	 */
	public String getCurrent()
	{
		return this.current;
	}

	/**
	 * Returns the character in the first position in current. This character is
	 * used to determine in which direction to proceed in a database.
	 * <p/>
	 * @return the first character of {@code current}
	 */
	public String front()
	{
		return "" + current.charAt(0);
	}

	/**
	 * Strips the first character of {@code current} by means of a substring
	 * call.
	 * <p/>
	 * @return the cropped iteration of {@code current}
	 */
	public DNASequence crop()
	{
		current = current.substring(1, current.length());
		return this;
	}

	/**
	 * Crops {@code current} by {@code depth} many 0-based positions from the
	 * front. This function assumes the caller utilizes a zero-based indexing
	 * system to handle these sequences so no compensation is necessary unless
	 * utilizing any other integer-based indexing system.
	 * <p/>
	 * @param depth the depth at which to crop
	 */
	public void cropAt(int depth)
	{
		//IMPORTANT! Calculations based on 0-based indexing!
		int actual = depth;
		if (depth >= current.length())
		{
			actual = current.length() - 1;
		}
		else if (actual < 0)
		{
			actual = 0;
		}
		current = current.substring(actual, current.length());
	}

	/**
	 * For easier database operations, each sequence should have {@code $}
	 * appended to the end of its {@code current} member. This easily denotes
	 * the end of a sequence rather than checking for size.
	 */
	public void terminate()
	{
		if (!sequence.endsWith("$"))
		{
			current += "$";
		}
	}

	/**
	 * Strips {@code $} from this sequence, if it is at the end. When searching,
	 * {@code $} can be used to denote an explicit match rather than a close
	 * match or prefix search.
	 */
	public void unterminate()
	{
		if (sequence.endsWith("$"))
		{
			sequence = sequence.substring(0, sequence.length() - 1);
		}
	}

	/**
	 * Restores {@code current} to the original state of {@code sequence}. This
	 * allows a sequence to be recycled for other operations.
	 */
	public void restore()
	{
		current = sequence;
	}

	/**
	 * Returns the length of {@code current}.
	 * <p/>
	 * @return the length of {@code current}
	 */
	public int length()
	{
		return current.length();
	}

	/**
	 * Returns the length of {@code sequence}, the true length of this sequence.
	 * <p/>
	 * @return the length of {@code sequence}
	 */
	public int literalLength()
	{
		return sequence.length();
	}

	/**
	 * Determines if this {@code DNASequence} equals {@code other}. These two
	 * {@code DNASequences} are equal iff their {@code sequence} members are
	 * equal. {@code ignoreTerminator} denotes whether or not {@code $} affects
	 * equivalency. This is {@code true} if an explicit match is desired and
	 * {@code $} has possibly been used previously to terminate this sequence,
	 * or the sequence was constructed with a {@code $} suffix.
	 * <p/>
	 * @param other            the other {@code DNASequence} to compare
	 * @param ignoreTerminator {@code true} if {@code $} does not affect
	 *                            equivalency, {@code false} otherwise
	 * <p/>
	 * @return {@code true} iff this {@code DNASequence} and {@code other} have
	 *            equivalent {@code sequence} members, {@code false} otherwise
	 */
	public boolean equals(DNASequence other, boolean ignoreTerminator)
	{
		if (ignoreTerminator)
		{
			this.unterminate();
			other.unterminate();
		}
		return this.sequence.equals(other.sequence);
	}

	@Override
	public String toString()
	{
		return this.sequence;
	}

	/**
	 * Returns a String representation of sequence base occurrences in the form
	 * of occurrence statistics. For example, a sequence of {@code AAAAGGTC}
	 * will return the following statistics: <li>A(50.00)</li> <li>C(12.50)</li>
	 * <li>G(25.00)</li> <li>T(12.50)</li>
	 * <p/>
	 * @return the statistics of this sequence
	 */
	public String getStats()
	{
		double aCount = 0;
		double cCount = 0;
		double gCount = 0;
		double tCount = 0;
		double total = sequence.length();
		int i = 0;
		while (i < total)
		{
			char curr = sequence.charAt(i);
			if (curr == 'A')
			{
				aCount++;
			}
			else if (curr == 'C')
			{
				cCount++;
			}
			else if (curr == 'G')
			{
				gCount++;
			}
			else if (curr == 'T')
			{
				tCount++;
			}
			i++;
		}

		double perA = aCount / total * 100;
		double perC = cCount / total * 100;
		double perG = gCount / total * 100;
		double perT = tCount / total * 100;

		//use formatting to report values to 2 decimal places
		return String.format("A(%.2f), C(%.2f), G(%.2f), T(%.2f)", perA, perC, perG, perT);
	}
}
