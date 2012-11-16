
/**
 * {@code InternalNode} objects represent an internal node within a tree
 * structure. They are not {@link LeafNode} objects, nor are they
 * {@link TrieNode.FLYWEIGHT} objects. Instead, {@code InternalNode} objects
 * have five children that could be any of the three types of {@link TrieNode}.
 * <p/>
 * @author orionf22
 * @author rinaldi1
 */
public class InternalNode
		extends TrieNode
{

	@Override
	public boolean isLeaf()
	{
		return false;
	}

	@Override
	public boolean isFlyweight()
	{
		return false;
	}
}
