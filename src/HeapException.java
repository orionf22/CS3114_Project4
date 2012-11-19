
/**
 * A simple class used to denote any extending classes as a
 * {@code HeapException}.
 * <p/>
 * @author orionf22
 * @author rinaldi1
 */
public class HeapException
		extends Exception
{

	/**
	 * Delegate to parent.
	 */
	public HeapException()
	{
		super();
	}

	/**
	 * Delegate to parent.
	 * <p/>
	 * @param message the String message to show with the error
	 */
	public HeapException(String message)
	{
		super(message);
	}
}
