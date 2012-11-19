
/**
 * Simple {@link Exception} that is thrown when a method attempts to operate on
 * an empty heap.
 * <p/>
 * @author orionf22
 * @author rinaldi1
 */
public class IllegalHeapStateException
		extends HeapException
{

	/**
	 * Delegate to parent.
	 */
	public IllegalHeapStateException()
	{
		super();
	}

	/**
	 * Delegate to parent.
	 * 
	 * @param message a String message to show with the exception
	 */
	public IllegalHeapStateException(String message)
	{
		super(message);
	}
}
