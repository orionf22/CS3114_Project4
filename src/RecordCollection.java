
/**
 * The {@code RecordCollection} interface is provided to a given class that
 * needs to make use of some storage medium to handle record management. This
 * allows for flexibility in design as any data structure can be used in an
 * implementing subclass to handle record I/O without the modification of any
 * other class making use of this interface.
 * <p/>
 * @author orionf22
 * @author rinaldi1
 */
public interface RecordCollection<E>
{

	/**
	 * Gets a record stored in position {@code index}.
	 * <p/>
	 * @param recordNum the index at which to retrieve a record
	 * <p/>
	 * @return the record stored at {@code index}
	 */
	public E get(int recordNum);

	/**
	 * Sets the record at position {@code recordNum} to {@code element}.
	 * <p/>
	 * @param element   the new record to use
	 * @param recordNum the index at which to replace
	 */
	public void set(E element, int recordNum);

    /**
     * Get the number of records in the collection
     * @return
     */
    public long getLength();
}
