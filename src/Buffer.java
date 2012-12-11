
/**
 * {@code Buffer} objects manage an array of bytes. They are capable of reading
 * and writing these bytes, using a {@code boolean} flag {@code isDirty} to
 * determine when the bytes have been modified and need to be updated in the
 * source. However, {@code Buffers} have no concept of what that source is, only
 * what bytes from that source it needs to manage.
 * <p/>
 * {@code Buffer} objects also have a {@code number} field to denote where a
 * {@code Buffer} falls in line within a source.
 * <p/>
 * @author orionf22
 * @author rinaldi1
 */
public class Buffer
{

	/**
	 * The bytes managed by this {@code Buffer}. This array is always the length
	 * of one block, defined by {@link BufferPool#BLOCK_SIZE}.
	 */
	private byte[] bytes;
	/**
	 * The number of this {@code Buffer}.
	 */
	private int number;
	/**
	 * Flag indicating when this {@code Buffer} has had modifications made to
	 * its bytes.
	 */
	private boolean isDirty;

	/**
	 * Constructs a new {@code Buffer} with a number of {@code blockNum} and
	 * managing {@code b}.
	 * <p/>
	 * @param blockNum the number of this {@code Buffer}
	 * @param b        the bytes managed by this {@code Buffer}
	 */
	public Buffer(int blockNum, byte[] b)
	{
		this.number = blockNum;
		this.bytes = b;
		isDirty = false;
	}

	/**
	 * Retrieves the byte array managed by this {@code Buffer}.
	 * <p/>
	 * @return the managed bytes
	 */
	public byte[] bytes()
	{
		return this.bytes;
	}

	/**
	 * Replaces select bytes in this {@code Buffer}, starting at {@code start}
	 * and replacing the corresponding bytes in {@code bytes} with those bytes
	 * in {@code b}.
	 * <p/>
	 * @param b     the bytes to use
	 * @param start the starting index within {@code bytes} at which overwriting
	 *                 will begin
	 */
	public void setBytes(byte[] b, int start)
	{
		int bIndex = 0;
		int size = b.length;
		for (int i = start; i < start + size; i++)
		{
			bytes[i] = b[bIndex];
			bIndex++;
		}
	}

	/**
	 * Sets one {@code byte} at position {@code pos}.
	 * <p/>
	 * @param b   the byte to set
	 * @param pos the position at which to set
	 */
	public void setByte(byte b, int pos)
	{
		bytes[pos] = b;
	}

	/**
	 * Sets the number of this {@code Buffer}. This allows a {@code Buffer}
	 * object to be recycled rather than garbage collected when pool replacement
	 * occurs.
	 * <p/>
	 * @param n the new number
	 */
	public void setNumber(int n)
	{
		this.number = n;
	}

	/**
	 * Gets a byte from {@link Buffer#bytes} at position {@code index}.
	 * <p/>
	 * @param index the index at which to retrieve
	 * <p/>
	 * @return the byte stored at {@code index}
	 */
	public byte get(int index)
	{
		//heapsort.output.println("Buffer, get, array size == "+ bytes.length);
		return bytes[index];
	}

	/**
	 * Returns the number of this {@code Buffer}.
	 * <p/>
	 * @return the number
	 */
	public int getNumber()
	{
		return this.number;
	}

	/**
	 * Marks this {@code Buffer} as {@code dirty}, indicating that changes have
	 * been made to its managed bytes that need to be reflected in the source.
	 */
	public void soil()
	{
		this.isDirty = true;
	}

	/**
	 * Resets the {@code isDIrty} flag. Only called when proper changes have
	 * been made to the source.
	 */
	public void clean()
	{
		this.isDirty = false;
	}

	/**
	 * Returns the {@code dirty} status of this {@code Buffer}.
	 * <p/>
	 * @return {@code true} is this {@code Buffer} is
	 *            {@code dirty}, {@code false} otherwise
	 */
	public boolean isDirty()
	{
		return this.isDirty;
	}
}
