/**
 * {@code MemoryPool} objects are simple {@code byte} arrays that store data
 * encoded in byte form. The {@code MemoryPool} itself does not know what these
 * bytes mean or what bytes correspond to what data, just that it is keeping
 * track of them all.
 * <p/>
 * When a request comes down to store bytes, the size of the request is marked
 * in an additional two-byte sequence prefixed to the actual byte sequence of
 * the information contained in the request. When a retrieval request comes down
 * later, a {@link MemHandle} will be used to examine a spot in memory that
 * prefixes actual data; this is the two-byte size sequence stored earlier. That
 * sequence will inform the requesting class how many bytes belong to that
 * portion of data.
 * <p/>
 * In the step-by-step process of handling memory requests: <ul><li>The
 * {@link Controller} queries its {@link RecordArray} to see if it can store a
 * new record.</li> <li>If space is available, the {@link MemManager} will
 * request enough free space from its {@link FreeBlockList}.</li><li>If there is
 * enough free space, it is removed from the {@link FreeBlockList} and a new
 * block of memory is passed to the {@code MemoryPool}.</li><li>The memory pool
 * uses the newly allocated space to store the request and the
 * {@link MemManager} passes up a {@link MemHandle} that can be used by
 * higher-level classes to retrieve information stored in the
 * {@code MemoryPool}.</li> </ul>
 * <p/>
 * @author orionf22
 * @author rinaldi1
 */
public class MemoryPool
{

	/**
	 * The size in bytes of the byte pool.
	 */
	private int size;
	/**
	 * The byte pool; where data is actually stored.
	 */
	private byte[] pool;

	/**
	 * Constructs a new {@code MemoryPool} of size {@code s}.
	 * <p/>
	 * @param s the size in bytes of this new {@code MemoryPool}
	 */
	public MemoryPool(int s)
	{
		this.size = s;
		pool = new byte[size];
	}

	/**
	 * Inserts a byte array, {@code stuff}, at {@code index} into the byte pool.
	 * <p/>
	 * @param stuff the byte array of data to insert
	 * @param index the index in the pool at which to insert
	 */
	public void insert(byte[] stuff, int index)
	{
	    //System.out.println("MemoryPool: insert: index = " + index);
	    //System.out.println("MemoryPool: insert: new byte[] s" + index);
		byte[] s = sizeToBytes((short) stuff.length);
		//System.out.println("MemoryPool: insert: pool[index]   = s[0]" + index);
		pool[index] = s[0];
		//System.out.println("MemoryPool: insert: pool[index+1] = s[1]" + index);
		pool[index + 1] = s[1];
		for (int i = index + 2; i < stuff.length + (index + 2); i++)
		{
		    //System.out.println("MemoryPool: insert: pool[i] = s[i - (index+2)]" + index);
			pool[i] = stuff[i - (index + 2)];
		}
	}

	/**
	 * Retrieves a byte array given a {@link MemHandle} {@code h} that denotes
	 * the starting location in the byte pool of the data.
	 * <p/>
	 * @param h the {@link MemHandle} addressing the two0byte size sequence of
	 *             the data to retrieve
	 * <p/>
	 * @return the byte array of retrieved data
	 */
	public byte[] get(MemHandle h)
	{
	    //System.out.println("MemoryPool: get");
	    //System.out.println("MemoryPool: index = h.getAddy");
		int index = h.getAddress();
		//System.out.println("MemoryPool: byte[] s = new byte[2]");
		byte[] s = new byte[2];
		//System.out.println("MemoryPool: s0 = pool[index]");
		s[0] = pool[index];
		//System.out.println("MemoryPool: s1 = pool[index+1]");
		s[1] = pool[index + 1];
		//System.out.println("MemoryPool: newSize = bytesToSize(s) " + bytesToSize(s));
		int newSize = bytesToSize(s);
		//System.out.println("MemoryPool: byte[] ret = new byte[newSize] " + newSize);
		byte[] ret = new byte[newSize];

		for (int i = index + 2; i < newSize + index + 2; i++)
		{
			ret[i - (index + 2)] = pool[i];
		}
		return ret;
	}

	/**
	 * Gets only the two-byte size sequence as an int value. This is used when
	 * only the byte size of a memory request is needed, not the actual data
	 * itself.
	 * <p/>
	 * @param h the {@link MemHandle} addressing the two-byte size sequence to
	 *             return
	 * <p/>
	 * @return the int size
	 */
	public int getByteSizeSquence(MemHandle h)
	{
		int index = h.getAddress();
		byte[] bytes =
		{
			pool[index], pool[index + 1]
		};
		return bytesToSize(bytes);
	}

	/**
	 * Removes bytes from the pool by zeroing out the two-byte size sequence.
	 * This makes any proceeding bytes originally belonging to the sequence
	 * inaccessible so they can be overwritten by future memory storages.
	 * <p/>
	 * @param h the {@link MemHandle} addressing the two-byte size sequence
	 * <p/>
	 * @return the size in bytes of the removed data
	 */
	public int remove(MemHandle h)
	{
		int index = h.getAddress();
		byte[] bytes =
		{
			pool[index], pool[index + 1]
		};
		int ret = bytesToSize(bytes);
		pool[index] = 0;
		pool[index + 1] = 0;
		return ret;
	}

	/**
	 * Converts a {@code short} value, {@code s} to a two0byte size sequence.
	 * This is used as a prefix to all stored data byte arrays to denote the
	 * actual byte length of stored data. All {@link MemHandle} references
	 * address this two-byte sequence and higher functions use the size to
	 * determine how many bytes belong to the pertinent data request.
	 * <p/>
	 * @param s the size of the stored data
	 * <p/>
	 * @return a two-byte sequence of the converted size
	 */
	public static byte[] sizeToBytes(short s)
	{
		byte[] ret = new byte[2];
		ret[0] = (byte) (s >> 8);
		ret[1] = (byte) (s);
		return ret;
	}

	/**
	 * Converts a two-byte array marking the size of stored data to a primitive
	 * {@code int}. {@code s} should be, at minimum, of size 2, and preferably
	 * no larger.
	 * <p/>
	 * @param s the two-byte size sequence
	 * <p/>
	 * @return the {@code int} size
	 */
	public static int bytesToSize(byte[] s)
	{
		return (s[0] << 8) + s[1];
	}

	/**
	 * Gets the size in bytes
	 * @return size
	 */
	public int getSize()
	{
	    return size;
	}

	/**
	 * Copies the values from {@code oldPool} into pool.
	 * 
	 * @param oldPool the original pool, acting as the source
	 */
	public void copyPoolFrom(MemoryPool oldPool)
	{
	    byte[] oldArray = oldPool.pool;
		System.arraycopy(oldArray, 0, pool, 0, oldPool.getSize());
	}
}
