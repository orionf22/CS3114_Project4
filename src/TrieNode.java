
/**
 * The {@code TrieNode} abstract class marks an implementing subclass a node to
 * operate upon in a {@link DNATrie}. Any class implementing this super class
 * must be able to distinguish leaf nodes.
 * <p/>
 * To avoid wasting precious space with useless empty nodes, or to preclude
 * forcing {@code null} checks, any node that should be empty or {@code null} is
 * instead directed to {@link #FLYWEIGHT}. This means one, easy check can be
 * made to determine if a node is empty or not.
 * <p/>
 * @author orionf22
 * @author rinaldi1
 */
public abstract class TrieNode
{
	/**
	 * Flyweight design; all empty nodes point to this single object.
	 */
	protected static TrieNode.FLYWEIGHT FLYWEIGHT = new TrieNode.FLYWEIGHT();
	/**
	 * The A subtree.
	 */
	protected TrieNode A = FLYWEIGHT;
	/**
	 * The C subtree.
	 */
	protected TrieNode C = FLYWEIGHT;
	/**
	 * The G subtree.
	 */
	protected TrieNode G = FLYWEIGHT;
	/**
	 * The T subtree.
	 */
	protected TrieNode T = FLYWEIGHT;
	/**
	 * The $ subtree (sequence terminator).
	 */
	protected TrieNode $ = FLYWEIGHT;

	/**
	 * Denotes this {@code TrieNode} as a leaf node.
	 * <p/>
	 * @return {@code true} if this {@code TrieNode} is a leaf node,
	 *            {@code false} otherwise
	 */
	public abstract boolean isLeaf();

	/**
	 * Denotes this {@code TrieNode} as the {@link DNATrie#FLYWEIGHT}.
	 * <p/>
	 * @return {@code true} if this {@code TrieNode} is the
	 *            {@link DNATrie#FLYWEIGHT}, {@code false} otherwise
	 */
	public abstract boolean isFlyweight();

	/**
	 * Calculates the number of non-empty nodes {@code node} owns in its
	 * subtrees. If {@code node} is at level {@code d}, all nodes below
	 * {@code node} starting at level {@code d} will be counted. Nodes can only
	 * be counted if they are a child of {@code node}.
	 * <p/>
	 * @param node the {@code TrieNode} representing the root of a subtree
	 * <p/>
	 * @return the number of non-empty nodes in the subtree
	 */
	public static int sizeOf(TrieNode node)
	{
		if (node == DNATrie.FLYWEIGHT)
		{
			return 0;
		}
		else
		{
			return 1 + sizeOf(node.A) + sizeOf(node.C) + sizeOf(node.G)
					+ sizeOf(node.T) + sizeOf(node.$);
		}
	}

	/**
	 * Determines how many non-flyweight children this {@code TrieNode} owns.
	 * Every node is assumed to have five non-flyweight children; each child
	 * node is then examined. If the child is a flyweight, the total number of
	 * children is decremented. The end result is the number of non-flyweight
	 * children.
	 * <p/>
	 * @return the total number of non-flyweight children of this
	 *            {@code TrieNode}
	 */
	public int childCount()
	{
		int ret = 5;
		if (this.A == DNATrie.FLYWEIGHT)
		{
			ret--;
		}
		if (this.C == DNATrie.FLYWEIGHT)
		{
			ret--;
		}
		if (this.G == DNATrie.FLYWEIGHT)
		{
			ret--;
		}
		if (this.T == DNATrie.FLYWEIGHT)
		{
			ret--;
		}
		if (this.$ == DNATrie.FLYWEIGHT)
		{
			ret--;
		}
		return ret;
	}

	/**
	 * Flyweight design; all empty (and thus {@code null}) nodes point to a
	 * single {@code FLYWEIGHT} node, {@link DNATrie#FLYWEIGHT}.
	 */
	protected static class FLYWEIGHT
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
			return true;
		}
	}
}
