
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * {@code BufferPool} objects utilize a {@link LinkedList} to manage a set
 * number of {@link Buffer} objects for the purposes of reading and writing data
 * from and to a given source file without having to make disk accesses. The
 * {@code BufferPool} allows portions of a source file to be kept in main memory
 * for faster I/O operations.
 * <p/>
 * A standard {@link LinkedList} is used to manage in-memory {@link Buffer}
 * objects. The <b>Least Recently Used</b> scheme is used to manage the pool
 * list. Source file I/O requests go through the {@code BufferPool}, which
 * supports direct reading and writing of the source using a
 * {@link RandomAccessFile} through the implementation of {@link Buffer}
 * objects.
 * <p/>
 * @author orionf22
 * @author rinaldi1
 */
public class BufferPool
{

	/**
	 * This {@code BufferPool's} {@link LinkedList}.
	 */
	private LinkedList<Buffer> pool;
	/**
	 * The current number of managed {@link Buffer} objects.
	 */
	private int size;
	/**
	 * This {@code BufferPool's} source.
	 */
	private RandomAccessFile file;
	/**
	 * The maximum number of {@code Buffer} objects this {@code BufferPool} can
	 * manage at one time.
	 */
	private int POOL_COUNT;
	/**
	 * A running count of the number of cache hits.
	 */
	private int CACHE_HITS;
	/**
	 * A running count of the number of cache misses.
	 */
	private int CACHE_MISSES;
	/**
	 * A running count of the number of disk reads.
	 */
	private int DISK_READS;
	/**
	 * A running count of the number of disk writes.
	 */
	private int DISK_WRITES;
	/**
	 * The static size of blocks within the source, in bytes. For Project 3,
	 * this is 4096.
	 */
	public static final int BLOCK_SIZE = 4096;

	/**
	 * Constructs a new {@code BufferPool} with space for {@code numBuffers}
	 * using {@code file}. By default, {@code file} is used to create a new
	 * {@link RandomAccessFile} set to {@code rw} mode.
	 * <p/>
	 * @param numBuffers the number of {@link Buffer Buffers} this pool will
	 *                      manage
	 * @param file       the {@link File} from which to read and write
	 * <p/>
	 * @throws FileNotFoundException
	 */
	public BufferPool(int numBuffers, File file) throws FileNotFoundException
	{
		//heapsort.output.println("new BufferPool");
		pool = new LinkedList<>();
		POOL_COUNT = numBuffers;
		this.file = new RandomAccessFile(file, "rw");
		CACHE_HITS = 0;
		CACHE_MISSES = 0;
		DISK_READS = 0;
		DISK_WRITES = 0;
	}

	/**
	 * Retrieves a byte array from the pool's source file, starting at position
	 * {@code start}. The returned array is always the size of
	 * {@link IntegerCollection#RECORD_SIZE}. Bytes from the source are acquired
	 * byte-by-byte from the right {@link Buffer}.
	 * <p/>
	 * @param start the location within the source from which to start reading
	 *                 bytes
	 * <p/>
	 * @return a byte array containing the bytes from the source
	 * <p/>
	 * @throws IOException
	 */
	public byte[] get(int start) throws IOException
	{
		byte[] ret = new byte[IntegerCollection.RECORD_SIZE];
		int retIndex = 0;
		for (int i = start; i < start + IntegerCollection.RECORD_SIZE; i++)
		{
			//determine which Buffer to look at
			int blockNum = i / BLOCK_SIZE;
			Buffer buff = retrieve(blockNum, blockNum * BLOCK_SIZE);
			ret[retIndex] = buff.get(i - (blockNum * BLOCK_SIZE));
			retIndex++;
		}
		return ret;
	}

	/**
	 * Sets {@code bytes} to a {@link Buffer}. This occurs when a change has
	 * been made elsewhere and needs to be stored in the source. When bytes are
	 * assigned to a {@link Buffer}, it is marked as {@code dirty} and will
	 * result in {@code bytes} overwriting the bytes in the corresponding
	 * position in the source.
	 * <p/>
	 * {@code start} denotes the location within the source at which bytes will
	 * be overwritten by the information contained within {@code bytes}.
	 * {@code start} is also used to retrieve the proper {@link Buffer} from the
	 * pool.
	 * <p/>
	 * @param bytes the bytes to assign to a {@link Buffer}
	 * @param start the starting index in the source at which to overwrite
	 * <p/>
	 * @throws IOException
	 */
	public void set(byte[] bytes, int start) throws IOException
	{
		//Determine which Buffer to get
		int blockNum = start / BLOCK_SIZE;
		Buffer buff = retrieve(blockNum, blockNum * BLOCK_SIZE);
		int newStart = start;
		//this check ensures the request index is always relative to the 
		//Buffer's byte array, NOT the source's array. Without this check, a
		//request to position 5000 (in Buffer 01) would result in a request in
		//Buffer 01's byte array at 5000, generating an out of bounds exception
		if (blockNum != 0)
		{
			newStart = start % BLOCK_SIZE;
		}
		buff.setBytes(bytes, newStart);
		buff.makeDirty();
	}

	/**
	 * Flushes all {@link Buffers} in the pool.
	 * <p/>
	 * @throws IOException
	 */
	public void flush() throws IOException
	{
		for (Buffer buff : pool)
		{
			if (buff.isDirty())
			{
				setBytesInFile(buff.bytes(), buff.getNumber() * BLOCK_SIZE);
				buff.clean();
			}
		}
	}

	/**
	 * Closes the source file stream.
	 * <p/>
	 * @throws IOException
	 */
	public void closeSourceStream() throws IOException
	{
		file.close();
	}

	/**
	 * Get the right {@link Buffer} from the pool given {@code blockNum}. If the
	 * desired {@link Buffer} is not already in the pool, it must be fetched. If
	 * the pool is already holding the maximum number of {@link Buffer Buffers},
	 * as defined by {@link BufferPool#POOL_COUNT POOL_COUNT}, then the
	 * {@link Buffer} at the end of the list is removed and the new
	 * {@link Buffer} added to the front. If the removed {@link Buffer} is
	 * marked as {@code dirty}, bytes in the source are modified.
	 * <p/>
	 * If the desired {@link Buffer} is already in the pool, a {@code cache hit}
	 * occurs. {@link BufferPool#CACHE_HITS CACHE_HITS} is incremented.
	 * <p/>
	 * @param blockNum if the desired {@link Buffer} is already in the pool, the
	 *                    {@link Buffer} with this number will be returned
	 * @param start    the starting index in the source at which to read data
	 * <p/>
	 * @return the desired {@link Buffer}
	 * <p/>
	 * @throws IOException
	 * @see BufferPool#setBytesInFile(byte[], int)
	 */
	private Buffer retrieve(int blockNum, int start) throws IOException
	{
		//Iterate through the pool for speed gains (as opposed to using a 
		//for-each loop
		ListIterator<Buffer> iter = pool.listIterator();
		//first search the pool for the right Buffer
		while (iter.hasNext())
		{
			Buffer buff = iter.next();
			//Match!
			if (buff.getNumber() == blockNum)
			{
				iter.remove();
				pool.addFirst(buff);
				CACHE_HITS++;
				return buff;
			}
		}
		//not in the pool, so add it
		Buffer buff = addBuffer(blockNum, start);
		return buff;
	}

	/**
	 * Adds a {@link Buffer} not already managed to the pool using the <b>Least
	 * Recently Used scheme</b>. If the pool is not full, then the desired
	 * {@link Buffer} is simply added to the front of the list. Otherwise the
	 * last {@link Buffer} is removed from the list and recycled as the "new"
	 * one and added to the front. If the removed {@link Buffer} is marked as
	 * {@code dirty}, then bytes in the source are modified.
	 * <p/>
	 * As this method is only invoked when a desired {@link Buffer} is not in
	 * the pool, a {@code cache miss} occurs.
	 * {@link BufferPool#CACHE_MISSES CACHE_MISSES} is incremented.
	 * <p/>
	 * @param blockNum the number of the desired {@link Buffer}
	 * @param start    the starting index (within the source) of the new
	 *                    {@link Buffer Buffer's} bytes
	 * <p/>
	 * @return the new {@link Buffer} added to the pool
	 * <p/>
	 * @throws IOException
	 * @see BufferPool#setBytesInFile(byte[], int)
	 */
	private Buffer addBuffer(int blockNum, int start) throws IOException
	{
		Buffer buff;
		//if the pool is full, remove the last Buffer, setting bytes if the
		//Buffer is dirty
		if (size == POOL_COUNT)
		{
			buff = pool.removeLast();
			if (buff.isDirty())
			{
				setBytesInFile(buff.bytes(), buff.getNumber() * BLOCK_SIZE);
				buff.clean();
			}
			//decrement size, knowing it will be incremented next anyway. This
			//is done so the size is always incremented properly; if the pool is
			//not full then size still needs to be incremented when a new Buffer
			//is added
			size--;
			//reuse this Buffer and its byte array rather than allocating a new 
			//array
			getBytesFromFile(buff.bytes(), start);
			buff.setNumber(blockNum);
		}
		//the pool is not full so a new Buffer is needed
		else
		{
			buff = new Buffer(blockNum,
					getBytesFromFile(new byte[BLOCK_SIZE], start));
		}
		size++;
		pool.addFirst(buff);
		CACHE_MISSES++;
		return buff;
	}

	/**
	 * Retrieves a bytes from the source starting at {@code start} and placed
	 * into {@code ret}. Data is read from disk in this method as bytes are
	 * accessed directly from the source file (stored on disk).
	 * {@link BufferPool#DISK_READS DISK_READS} is incremented.
	 * <p/>
	 * A byte array is used from elsewhere (as {@code ret}) rather than
	 * allocating a new array of size {@link BufferPool#BLOCK_SIZE BLOCK_SIZE}.
	 * <p/>
	 * @param start the starting index at which to acquire bytes from the source
	 * <p/>
	 * @return the acquired bytes from the source
	 * <p/>
	 * @throws IOException
	 */
	private byte[] getBytesFromFile(byte[] ret, int start) throws IOException
	{
		//navigate to the right position in the source
		file.seek(start);
		file.read(ret, 0, BLOCK_SIZE);
		DISK_READS++;
		return ret;
	}

	/**
	 * Modifies the contents of the source starting at {@code start}. The
	 * contents of {@code bytes} are used to overwrite existing information.
	 * Data is written to the source file (stored on disk).
	 * {@link BufferPool#DISK_WRITES DISK_WRITES} is incremented.
	 * <p/>
	 * @param bytes the bytes to write
	 * @param start the starting index at which to write
	 * <p/>
	 * @throws IOException
	 */
	private void setBytesInFile(byte[] bytes, int start) throws IOException
	{
		//navigate to proper position
		file.seek(start);
		file.write(bytes);
		DISK_WRITES++;
	}

	/**
	 * Retrieves the number of cache hits this {@code BufferPool} generated.
	 * <p/>
	 * @return the cache hit count
	 */
	public int getCacheHits()
	{
		return this.CACHE_HITS;
	}

	/**
	 * Retrieves the number of cache misses this {@code BufferPool} generated.
	 * <p/>
	 * @return the cache miss count
	 */
	public int getCacheMisses()
	{
		return this.CACHE_MISSES;
	}

	/**
	 * Retrieves the number of disk reads this {@code BufferPool} made.
	 * <p/>
	 * @return the disk read count
	 */
	public int getDiskReads()
	{
		return this.DISK_READS;
	}

	/**
	 * Retrieves the number of disk writes this {@code BufferPool} made.
	 * <p/>
	 * @return the disk write count
	 */
	public int getDiskWrites()
	{
		return this.DISK_WRITES;
	}
}
