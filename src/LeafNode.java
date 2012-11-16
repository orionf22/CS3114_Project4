
/**
 * {@code LeafNode} objects store a {@link MemHandle} and integer value. Both
 * are used to retrieve a stored piece of information from a memory pool.
 * <p/>
 * @author orionf22
 * @author rinaldi1
 */
public class LeafNode
		extends TrieNode
{

	/**
	 * The stored {@link MemHandle}.
	 */
	private MemHandle handle;
	/**
	 * The true length of the stored information referenced by {@code handle}.
	 */
	private int length;

	/**
	 * Constructs a new {@code LeafNode} from {2code h} and {@code l}.
	 * 
	 * @param h the {@link MemHandle} to use
	 * @param l the true length of the stored information
	 */
	public LeafNode(MemHandle h, int l)
	{
		this.handle = h;
		this.length = l;
	}

	/**
	 * Returns the stored {@link MemHandle}.
	 * 
	 * @return the {@link MemHandle}
	 */
	public MemHandle getHandle()
	{
		return this.handle;
	}

	/**
	 * Returns the stored true length of information.
	 * 
	 * @return the true length of stored information.
	 */
	public int getLiteralLength()
	{
		return this.length;
	}

	@Override
	public boolean isLeaf()
	{
		return true;
	}

	@Override
	public boolean isFlyweight()
	{
		return false;
	}
}
