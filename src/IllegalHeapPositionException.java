/**
 * Basic {@link Exception} thrown when an attempt is made to utilize an illegal
 * Heap position. An illegal heap position is defined as any position less than
 * zero or greater than the current number of elements in the heap.
 * 
 * @author orionf22
 * @author rinaldi1
 */
public class IllegalHeapPositionException
	extends HeapException
{
	/**
	 * Default to super constructor, no custom error message.
	 */
	public IllegalHeapPositionException()
	{
		super();
	}
	
	/**
	 * Default to super constructor, use custom error message.
	 * @param message 
	 */
	public IllegalHeapPositionException(String message)
	{
		super(message);
	}
}
