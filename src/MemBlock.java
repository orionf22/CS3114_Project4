/**
 * {@code MemBlock} objects wrap two primitive int values that denote a location
 * in memory and size allocated to store something in a given memory chunk.
 * 
 * @author orionf22
 * @author rinaldi1
 */
public final class MemBlock
{
	/**
	 * The size in bytes of this {@code MemBlock}.
	 */
	private int size;
	/**
	 * The starting index/address of this {@code MemBlock}.
	 */
	private int address;
	
	/**
	 * Constructs a new {@code MemBlock} from {@code s} and {@code h}.
	 * 
	 * @param h the starting index/address of this {@code MemBlock}
	 * @param s the size of this {@code MemBlock}
	 */
	public MemBlock(final int h, final int s)
	{
		this.address = h;
		this.size = s;
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
	
	/**
	 * Retrieves the size in bytes of this {@code MemBlock}.
	 * 
	 * @return the size of this {@code MemBlock}
	 */
	public int getSize()
	{
		return this.size;
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other != null && other instanceof MemBlock)
		{
			MemBlock second = (MemBlock) other;
			if (second.hashCode() == this.hashCode() && 
					second.getAddress() == this.getAddress()
					&& second.getSize() == this.getSize())
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		int hash = 7;
		hash = 29 * hash + this.size;
		hash = 29 * hash + this.address;
		return hash;
	}
	
	@Override
	public String toString()
	{
		return this.address + ":" + this.size;
	}
}
