
/**
 * Manages heap sorting according to the max-heap standards. Sorting time is
 * recorded in {@code time}.
 * <p/>
 * @author orionf22
 * @author rinaldi1
 */
public class HeapSorter
{

	/**
	 * The time it took for the last sort in milliseconds.
	 */
	private long time = -1;
	/**
	 * This {@code HeapSorter's} {@link RecordCollection}, which interfaces
	 * between this class and a storage medium, such as a specialized array or
	 * buffer pool.
	 */
	private RecordCollection<HeapRecord> collection;

	/**
	 * Constructs a new {@code HeapSorter} using {@code collection}.
	 * <p/>
	 * @param collection the interface between this sorter and the storage
	 *                      medium
	 */
	public HeapSorter(RecordCollection<HeapRecord> collection)
	{
		this.collection = collection;
	}

	/**
	 * Returns the last sorting time in milliseconds. A negative value implies
	 * no sorting occurred.
	 * <p/>
	 * @return the sort time
	 */
	public long getSortTime()
	{
		return time;
	}

	/**
	 * Sorts the heap.
	 * <p/>
	 * @throws HeapException
	 */
	public void sort() throws HeapException
	{
		// Get the initial time
		long startTime = System.currentTimeMillis();

		MaxHeap<HeapRecord> H = new MaxHeap<>(collection, 
				collection.getLength(), collection.getLength());
		for (int i = 0; i < collection.getLength(); i++)
		{
			//removeMax places max at end of heap
			H.removeMax();
		}

		// Get the end time
		long endTime = System.currentTimeMillis();
		// Calculate the total time
		time = endTime - startTime;
	}
}
