
import java.util.Collection;

/**
 * {@code DNATrie} objects are 5-way branching trees that store
 * {@link MemHandle} and integer objects but are sorted by {@link DNASequence}.
 * Each node can branch into five children: <ul><li>A</li> <li>C</li> <li>G</li>
 * <li>T</li> <li>$</li></ul>
 * <p/>
 * As a sequence is used during tree navigation, be it for inserting, removal,
 * or searching, it determines which branch to take based on the current value
 * of the leading character in that sequence.
 * <p/>
 * {@code DNATrie} objects support inserting, removal, and explicit and prefix
 * searching. Explicit searching attempts to find an exact match for a given
 * sequence while prefix searching attempts to find the first node that contains
 * the given sequence, but as a prefix. For example, a prefix search for
 * {@code AAA} will return the first node that contains a reference to a
 * sequence whose first three characters are {@code AAA}. Because of Trie rules,
 * any node that is a child of this first node also has {@code AAA} as a prefix.
 * <p/>
 * @author orionf22
 * @author rinadli1
 */
public class DNATrie
{

	/**
	 * The root node of this tree.
	 */
	private TrieNode root;
	
	private MemHandle rootHandle;
	/**
	 * Flyweight design; all empty nodes point to this single object.
	 */
	protected static final TrieNode.FLYWEIGHT FLYWEIGHT = TrieNode.FLYWEIGHT;
	/**
	 * Anything not currently in use by the tree is stored on disk via a
	 * {@link MemManager}.
	 */
	private MemManager manager;
	
	private NodeCodec codec;
	/**
	 * The size of this tree.
	 */
	private int size;
	/**
	 * Generic print call.
	 */
	public static final int JUST_DO_IT_SON = 0;
	/**
	 * Print according to length.
	 */
	public static final int BY_LENGTH = 1;
	/**
	 * Print according to occurrence statistics.
	 */
	public static final int BY_STATS = 2;

	/**
	 * Initializes this {@code DNATrie} with a {@code root} pointing to
	 * {@link #FLYWEIGHT}.
	 * <p/>
	 * @param manager the {@link MemManager} to use to store and retrieve data
	 *                   to and from disk
	 */
	public DNATrie(MemManager manager)
	{
		root = FLYWEIGHT;
		this.manager = manager;
		this.codec = new NodeCodec();
		rootHandle = manager.insert(codec.encode(root));
	}

	/**
	 * Returns a the number of nodes visited during a search for
	 * {@code sequence}. Any match, or matches, as prefix searching is handled
	 * here, are added to {@code c}.
	 * <p/>
	 * @param sequence the {@link DNASequence} to find
	 * @param c        the {@link COllection} containing any and all matches
	 * <p/>
	 * @return the number of nodes visited during this search
	 */
	public int get(DNASequence sequence, Collection<String> c)
	{
		return get(root, sequence, c);
	}

	/**
	 * Returns a the number of nodes visited during a search for
	 * {@code sequence}. Any match, or matches, as prefix searching is handled
	 * here, are added to {@code c}.
	 * <p/>
	 * @param node     the node to examine
	 * @param sequence the {@link DNASequence} to find
	 * @param c        the {@link COllection} containing any and all matches
	 * <p/>
	 * @return the number of nodes visited during this search
	 */
	private int get(TrieNode node, DNASequence sequence, Collection<String> c)
	{
		//add sequence contained here if matching
		if (node.isLeaf())
		{
			LeafNode leaf = (LeafNode) node;
			DNASequence seq = new DNASequence(DNAFile.controller.retrieve(leaf.getHandle(), leaf.getLiteralLength()));
			if (seq.equals(sequence, true))
			{
				c.add(seq.getSequence());
			}
			return 1;
		}
		//continue searching
		else if (sequence.length() > 0 && !node.isFlyweight())
		{
			switch (sequence.front())
			{
				case DNASequence.BASE_A:
					return 1 + get(node.A, sequence.crop(), c);
				case DNASequence.BASE_C:
					return 1 + get(node.C, sequence.crop(), c);
				case DNASequence.BASE_G:
					return 1 + get(node.G, sequence.crop(), c);
				case DNASequence.BASE_T:
					return 1 + get(node.T, sequence.crop(), c);
				case DNASequence.TERMINATOR:
					return 1 + get(node.$, sequence.crop(), c);
				default:
					return 1;
			}
		}
		return loadPrefixes(node, c);
	}

	/**
	 * Attempts to fetch a {@link TrieNode} with a {@link MemHandle} referencing
	 * {@code sequence}.
	 * <p/>
	 * @param sequence the {@link DNASequence} to find
	 * <p/>
	 * @return a {@link LeafNode} referencing {@code sequence} if
	 *            {@code sequence} exists, otherwise {@link DNATrie#FLYWEIGHT} if
	 *            the sequence does not exist
	 */
	public TrieNode fetch(DNASequence sequence)
	{
		sequence.terminate();
		return fetch(root, sequence);
	}

	/**
	 * Attempts to fetch a {@link TrieNode} with a {@link MemHandle} referencing
	 * {@code sequence}.
	 * <p/>
	 * @param node     the node to examine
	 * @param sequence the {@link DNASequence} to find
	 * <p/>
	 * @return a {@link LeafNode} referencing {@code sequence} if
	 *            {@code sequence} exists, otherwise {@link DNATrie#FLYWEIGHT} if
	 *            the sequence does not exist
	 */
	private TrieNode fetch(TrieNode node, DNASequence sequence)
	{
		//match not found
		if (node == DNATrie.FLYWEIGHT)
		{
			return node;
		}
		//possible match
		else if (node.isLeaf())
		{
			LeafNode leaf = (LeafNode) node;
			String seq = DNAFile.controller.retrieve(leaf.getHandle(), leaf.getLiteralLength());
			//match
			if (seq.equals(sequence.getSequence()))
			{
				return leaf;
			}
			return DNATrie.FLYWEIGHT;
		}
		//InternalNode
		else
		{
			switch (sequence.front())
			{
				case DNASequence.BASE_A:
					return fetch(node.A, sequence.crop());
				case DNASequence.BASE_C:
					return fetch(node.C, sequence.crop());
				case DNASequence.BASE_G:
					return fetch(node.G, sequence.crop());
				case DNASequence.BASE_T:
					return fetch(node.T, sequence.crop());
				case DNASequence.TERMINATOR:
					return fetch(node.$, sequence.crop());
				default:
					System.out.println("Error: getsize");
					return DNATrie.FLYWEIGHT;
			}
		}
	}

	/**
	 * Removes a given {@link DNASequence} from this tree. Searching begins at
	 * the {@code root} and traverses through the tree until a node containing a
	 * {@link MemHandle} referencing {@code sequence} is found. This node is
	 * returned only if this is the case; otherwise {@link #FLYWEIGHT} is
	 * returned.
	 * <p/>
	 * @param sequence the {@link DNASequence} to remove
	 * <p/>
	 * @return the {@link TrieNode} containing a {@link MemHandle} referencing
	 *            {@code sequence}, {@link #FLYWEIGHT} if no match if found
	 */
	public void remove(DNASequence sequence)
	{
		sequence.terminate();
		root = remove(root, sequence);
	}

	/**
	 * Removes a given {@link DNASequence} from this tree. Searching continues
	 * until a node containing a {@link MemHandle} referencing {@code sequence}
	 * is found. This node is returned only if this is the case; otherwise
	 * {@link #FLYWEIGHT} is returned.
	 * <p/>
	 * @param node     the node to examine
	 * @param sequence the {@link DNASequence} to remove
	 * <p/>
	 * @return the {@link TrieNode} containing a {@link MemHandle} referencing
	 *            {@code sequence}, {@link #FLYWEIGHT} if no match if found
	 */
	@SuppressWarnings("AssignmentToMethodParameter")
	private TrieNode remove(TrieNode node, DNASequence sequence)
	{
		//sequence does not exist
		if (node == DNATrie.FLYWEIGHT)
		{
			return node;
		}
		//sequence could be here
		else if (node.isLeaf())
		{
			LeafNode leaf = (LeafNode) node;
			String seq = DNAFile.controller.retrieve(leaf.getHandle(), leaf.getLiteralLength());
			//sequence is here; set node to FLYWEIGHT and return
			if (seq.equals(sequence.getSequence()))
			{
				node = DNATrie.FLYWEIGHT;
				size--;
				return node;
			}
		}
		//InternalNode; recurse through tree
		else
		{
			switch (sequence.front())
			{
				// change from node.x to node
				case DNASequence.BASE_A:
					//System.out.println("went to A");
					node.A = remove(node.A, sequence.crop());
					break;
				case DNASequence.BASE_C:
					//System.out.println("went to C");
					node.C = remove(node.C, sequence.crop());
					break;
				case DNASequence.BASE_G:
					//System.out.println("went to G");
					node.G = remove(node.G, sequence.crop());
					break;
				case DNASequence.BASE_T:
					//System.out.println("went to T");
					node.T = remove(node.T, sequence.crop());
					break;
				case DNASequence.TERMINATOR:
					//System.out.println("went to $");
					node.$ = remove(node.$, sequence.crop());
					break;
				default:
					break;
			}
			//These if blocks determine if collpasing should occur. Nodes should
			//collapse iff there is only one LeafNode linked to node. If there
			//is more than one leaf, no collpasing occurs.
			if (node.A.isLeaf() && node.C.isFlyweight() && node.G.isFlyweight()
					&& node.T.isFlyweight() && node.$.isFlyweight())
			{
				node = node.A;
			}
			else if (node.C.isLeaf() && node.A.isFlyweight() && node.G.isFlyweight()
					&& node.T.isFlyweight() && node.$.isFlyweight())
			{
				node = node.C;
			}
			else if (node.G.isLeaf() && node.A.isFlyweight() && node.C.isFlyweight()
					&& node.T.isFlyweight() && node.$.isFlyweight())
			{
				node = node.G;
			}
			else if (node.T.isLeaf() && node.A.isFlyweight() && node.C.isFlyweight()
					&& node.G.isFlyweight() && node.$.isFlyweight())
			{
				node = node.T;
			}
			else if (node.$.isLeaf() && node.A.isFlyweight() && node.C.isFlyweight()
					&& node.G.isFlyweight() && node.T.isFlyweight())
			{
				node = node.$;
			}
		}
		return node;
	}

	/**
	 * Inserts {@code h} into this tree by using {@code sequence} to determine
	 * where in the tree {@code h} must be placed in order to preserve Trie
	 * properties.
	 * <p/>
	 * @param h        the {@link MemHandle} to insert
	 * @param sequence the {@code DNASequence} to use to map
	 */
	public void insert(MemHandle h, DNASequence sequence)
	{
		//for easier insertion, append a $ to the end of the sequence to easily
		//identify sequence termination
		sequence.terminate();
		root = insert(root, h, sequence, sequence.literalLength(), 0);
	}

	/**
	 * Inserts {@code h} and {@code length} into a this tree. {@code sequence}
	 * is used to map out the position within the tree {@code h}. The function
	 * operates on recursion; base case is when {@link #FLYWEIGHT} is reached.
	 * This means this is the correct location for the inserting values.
	 * <p/>
	 * If an {@link InternalNode} is reached, branches must be examined based on
	 * the current leading character in {@code sequence}. If a {@link LeafNode}
	 * is reached, its existing values must be relocated and the leaf split
	 * (converting it into a new {@link InternalNode}). To accomplish this, the
	 * existing {@link MemHandle} and literal length are used by a
	 * {@link Controller} to query its {@link MemManager} for the actual
	 * sequence stored here. This sequence is then cropped based on the depth
	 * within the tree so it can be reinserted at the current level.
	 * <p/>
	 * For example, a sequence of {@code AGCT} at depth 2 would be retrieved in
	 * full and cropped to {@code CT} and reinserted as such. This allows the
	 * existing sequence to be properly reinserted at the same level as
	 * {@code sequence}. The original sequence is then inserted.
	 * <p/>
	 * @param node     the node to examine
	 * @param h        the {@link MemHandle} to store
	 * @param sequence the {@link DNASequence} to use
	 * @param length   the length of {@code sequence}
	 * @param depth    current depth of tree; used to determine cropping range
	 * <p/>
	 * @return the inserted node
	 */
	@SuppressWarnings("AssignmentToMethodParameter")
	private TrieNode insert(TrieNode node, MemHandle h, DNASequence sequence, int length, int depth)
	{
		//base case; insert here
		if (node == DNATrie.FLYWEIGHT)
		{
			//System.out.println("flyweight");
			size++;
			node = new LeafNode(h, length);
		}
		//split node as new InternalNode. Insert existing info and continue 
		//inserting original info
		else if (node.isLeaf())
		{
			//System.out.println("leaf " + node);
			LeafNode leaf = (LeafNode) node;
			node = new InternalNode();
			DNASequence seq = new DNASequence(DNAFile.controller.retrieve(leaf.getHandle(), leaf.getLiteralLength()));
			seq.terminate();
			seq.cropAt(depth);
			//System.out.println(seq + "; " + seq.getCurrent() + ": " + sequence + "; " + sequence.getCurrent() + " - " + depth);
			node = insert(node, h, sequence, length, depth);
			node = insert(node, leaf.getHandle(), seq, leaf.getLiteralLength(), depth);
		}
		//InternalNode
		else
		{
			//System.out.println("internal " + node);
			switch (sequence.front())
			{
				case DNASequence.BASE_A:
					//System.out.println("went to A");
					node.A = insert(node.A, h, sequence.crop(), length, depth + 1);
					break;
				case DNASequence.BASE_C:
					//System.out.println("went to C");
					node.C = insert(node.C, h, sequence.crop(), length, depth + 1);
					break;
				case DNASequence.BASE_G:
					//System.out.println("went to G");
					node.G = insert(node.G, h, sequence.crop(), length, depth + 1);
					break;
				case DNASequence.BASE_T:
					//System.out.println("went to T");
					node.T = insert(node.T, h, sequence.crop(), length, depth + 1);
					break;
				case DNASequence.TERMINATOR:
					//System.out.println("went to $");
					node.$ = insert(node.$, h, sequence.crop(), length, depth + 1);
					break;
				default:
					DNAFile.output.println("Error during insert");
					break;
			}
		}
		return node;
	}

	/**
	 * Returns the size of this {@code DNATrie}.
	 * <p/>
	 * @return the size of this tree
	 */
	public int capacity()
	{
		return this.size;
	}

	/**
	 * Gets the {@code root} node of this tree.
	 * <p/>
	 * @return the root of this tree
	 */
	public TrieNode getRoot()
	{
		return this.root;
	}

	/**
	 * Returns the number of nodes visited during a search, typically a prefix
	 * search. Any and all matches are added to {@code c}.
	 * <p/>
	 * This method does not need to search for a match. {@code node} is expected
	 * to be an "umbrella" node, meaning it is the first node in a tree that
	 * contains a prefix match. Because of Trie properties, any and all nodes
	 * below {@code node} must also contain the prefix, so a DFS is used to add
	 * each sequence to {@code c}.
	 * <p/>
	 * @param node the node to examine
	 * @param c    the {@link Collection} of matches
	 * <p/>
	 * @return the number of nodes visited
	 */
	private int loadPrefixes(TrieNode node, Collection<String> c)
	{
		//nothing here
		if (node == DNATrie.FLYWEIGHT)
		{
			return 1;
		}
		//add match
		else if (node.isLeaf())
		{
			LeafNode leaf = (LeafNode) node;
			DNASequence seq = new DNASequence(DNAFile.controller.retrieve(leaf.getHandle(), leaf.getLiteralLength()));
			c.add(seq.getSequence());
			return 1;
		}
		else
		{
			return 1 + loadPrefixes(node.A, c) + loadPrefixes(node.C, c)
					+ loadPrefixes(node.G, c) + loadPrefixes(node.T, c)
					+ loadPrefixes(node.$, c);
		}
	}
}
