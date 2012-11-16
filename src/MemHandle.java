/**
 * {@code MemHandle} objects wrap a primitive int value marking an address in
 * some memory unit. They are used to mark starting locations of certain data
 * chunks, and are provided to high-level classes to retrieve such data.
 * 
 * @author orionf22
 * @author rinaldi1
 */
public class MemHandle
{
	/**
	 * The address/index of this {@code MemHandle}.
	 */
	private int address;
	
	/**
	 * Construct a new {@code MemHandle} with an address denoted by {@code a}.
	 * 
	 * @param a the address to use
	 */
	public MemHandle(int a)
	{
		this.address = a;
	}
	
	/**
	 * Retrieves the address of this {@code MemBlock}.
	 * 
	 * @return the address of this {@code MemBlock}
	 */
	public int getAddress()
	{
		return this.address;
	}
}
