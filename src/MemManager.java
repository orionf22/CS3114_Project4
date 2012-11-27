
import java.io.File;


/**
 * The {@code MemManager} class controls how information is stored in memory.
 * They make use of a {@link MemoryPool} that stores the actual bytes of
 * information and a {@link FreeBlockList} that keeps track of free space.
 * <p/>
 * In the step-by-step process of handling memory requests: <ul><li>The
 * {@link Controller} queries its {@link RecordArray} to see if it can store a
 * new record.</li> <li>If space is available, the {@code MemManager} will
 * request enough free space from its {@link FreeBlockList}.</li><li>If there is
 * enough free space, it is removed from the {@link FreeBlockList} and a new
 * block of memory is passed to the {@link MemoryPool}.</li><li>The memory pool
 * uses the newly allocated space to store the request and the
 * {@code MemManager} passes up a {@link MemHandle} that can be used by
 * higher-level classes to retrieve information stored in the
 * {@link MemoryPool}.</li> </ul>
 * <p/>
 * @author orionf22
 * @author rinaldi1
 */
public class MemManager
{

	/**
	 * The {@link MemoryPool} managed by this {@code MemManager}. Actual data is
	 * stored here.
	 */
	private MemoryPool pool;
	
	private BufferPool bufferPool;
	/**
	 * The {@link FreeBlockList} managed by this {@code MemManager}. Free space
	 * is monitored here.
	 */
	private FreeBlockList freeBlocks;

	/**
	 * Constructs a new {@code MemManager} of the given size, {@code poolSize}.
	 * <p/>
	 * @param poolSize the size in bytes of this {@code MemManager}
	 */
	public MemManager(int poolSize, int blockSize, int buffers, File file)
	{
		this.pool = new MemoryPool(poolSize);
		this.bufferPool = new BufferPool(buffers, file, blockSize);
		this.freeBlocks = new FreeBlockList(poolSize);
	}

	/**
	 * Inserts a byte array into {@code pool}. If this function has been
	 * reached, the {@link Controller} has already determined that its
	 * {@link RecordArray} can store this request. The {@link FreeBlockList} is
	 * then queried for a free block large enough to store the request. If
	 * enough space exists, a {@link MemHandle} is passed up marking the address
	 * of this newly allocated memory. The {@link MemoryPool} then uses this
	 * address to begin inserting the two-byte size sequence and actual data.
	 * <p/>
	 * In the event that there is insufficient space within {@code pool}, 100
	 * bytes are successively added until enough free space exists to honor the
	 * request. Everything in the original pool is copied into the new pool and
	 * a new {@link MemBlock} of the recently added space is added to
	 * {@code freeList}. Merging is automatically handled.
	 * <p/>
	 * @param stuff the data to insert
	 * <p/>
	 * @return a {@link MemHandle} addressing the start of the two-byte size
	 *            sequence belonging to the data request
	 */
	public MemHandle insert(byte[] stuff)
	{
		//Important! Request an additional 2 bytes for the 2-byte size sequence
		MemHandle insertHandle = freeBlocks.getSpace(stuff.length + 2);
		if (insertHandle.getAddress() >= 0)
		{
			pool.insert(stuff, insertHandle.getAddress());
		}
		//Insufficient free space; continue to add 100 bytes until enough space 
		//exists
		else
		{
			//add 100 bytes
			int increaseSize = 100;
			int oldSize = pool.getSize();
			//Create a new MemoryPool with increased size
			MemoryPool newPool = new MemoryPool(oldSize + increaseSize);
			//Copy the data from the original pool to the new pool
			newPool.copyPoolFrom(pool);
			//Set pool to be the new pool
			pool = newPool;
			//Add the additional space to the FreeBlockList; important to NOT 
			//add 1 to the space request as the size (oldSize) is NOT zero-based 
			//but the freelist and pool ARe zero-based, thus making the +1 
			//unnecessary and incorrect
			freeBlocks.reclaimSpace(new MemHandle(oldSize), increaseSize);
			//Size increased; recursively call insert again till sufficient space
			return insert(stuff);
		}
		return insertHandle;
	}

	/**
	 * Removes an existing sequence of bytes from the {@code MemoryPool}.
	 * Instead of visiting each byte and clearing it, the two-byte size sequence
	 * is set to zero. This means any bytes previously owned by the data are now
	 * virtually inaccessible; they can be overwritten as needed.
	 * <p/>
	 * @param h the {@link MemHandle} addressing the two-byte size sequence of
	 *             the data to remove
	 */
	public int remove(MemHandle h)
	{
		int ret = pool.remove(h);
		//removing from the pool only returns the number of bytes needed to
		//store the actual record, not including the size sequence prefix, so
		//add 2 to the reclaim space request
		freeBlocks.reclaimSpace(h, ret + 2);
		//find block referenced by h
		//remove from mem pool; get size bytes first
		//modify free list to include recently freed space
		return ret;
	}

	/**
	 * Retrieves a byte sequence from the {@link MemoryPool} addressed by
	 * {@code h}.
	 * <p/>
	 * @param h the {@link MemHandle} addressing the two-byte size sequence of
	 *             the data to retrieve
	 * <p/>
	 * @return the byte array of retrieved information
	 */
	public byte[] get(MemHandle h)
	{
		return pool.get(h);
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
	public int getByteSizeSequence(MemHandle h)
	{
		return pool.getByteSizeSquence(h);
	}

	/**
	 * Returns a String representation of {@code freeBlocks}' free blocks.
	 * <p/>
	 * @see FreeBlockList#blocksToString()
	 * @return the String representation of free space
	 */
	public String getFreeBlocks()
	{
		return freeBlocks.blocksToString();
	}
}
