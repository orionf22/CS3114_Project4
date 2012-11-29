
import java.nio.ByteBuffer;
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
	private MemHandle root;
	/**
	 * Flyweight design; all empty nodes point to this single object.
	 */
	final MemHandle FLYWEIGHT;
	/**
	 * Anything not currently in use by the tree is stored on disk via a
	 * {@link MemManager}.
	 */
	private MemManager manager;
	private NodeCodec codec;
	private DNACodec DNACodec;
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
	 * Default length of a printed record before remaining characters will be
	 * cropped off. This improves output readability.
	 */
	private static final int DEFAULT_STRING_CROP_LENGTH = 40;

	/**
	 * Initializes this {@code DNATrie} with a {@code root} pointing to
	 * {@link #FLYWEIGHT}.
	 * <p/>
	 * @param manager the {@link MemManager} to use to store and retrieve data
	 *                   to and from disk
	 */
	public DNATrie(MemManager manager)
	{
		this.manager = manager;
		this.codec = new NodeCodec();
		FLYWEIGHT = manager.insert(codec.encode(new FLYWEIGHT()));
		root = FLYWEIGHT;
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
		TrieNode rt = loadNode(root);
		int ret = get(rt, sequence, c);
		root = saveNode(rt);
		return ret;
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
			DNASequence seq = new DNASequence(retrieve(leaf.getHandle(), leaf.getLiteralLength()));
			if (seq.equals(sequence, true))
			{
				c.add(seq.getSequence());
			}
			return 1;
		}
		//continue searching
		else if (sequence.length() > 0 && !node.isFlyweight())
		{
			InternalNode internal = (InternalNode) node;
			switch (sequence.front())
			{
				case DNASequence.BASE_A:
					return 1 + get(internal.getA(), sequence.crop(), c);
				case DNASequence.BASE_C:
					return 1 + get(internal.getC(), sequence.crop(), c);
				case DNASequence.BASE_G:
					return 1 + get(internal.getG(), sequence.crop(), c);
				case DNASequence.BASE_T:
					return 1 + get(internal.getT(), sequence.crop(), c);
				case DNASequence.TERMINATOR:
					return 1 + get(internal.get$(), sequence.crop(), c);
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
	public boolean fetch(DNASequence sequence)
	{
		sequence.terminate();
		TrieNode rt = loadNode(root);
		TrieNode got = fetch(rt, sequence);
		root = saveNode(rt);
		if (got.isLeaf())
		{
			return true;
		}
		return false;
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
		if (node.isFlyweight())
		{
			return node;
		}
		//possible match
		else if (node.isLeaf())
		{
			LeafNode leaf = (LeafNode) node;
			String seq = retrieve(leaf.getHandle(), leaf.getLiteralLength());
			//match
			if (seq.equals(sequence.getSequence()))
			{
				return leaf;
			}
			return loadNode(FLYWEIGHT);
		}
		//InternalNode
		else
		{
			InternalNode internal = (InternalNode) node;
			switch (sequence.front())
			{
				case DNASequence.BASE_A:
					return fetch(internal.getA(), sequence.crop());
				case DNASequence.BASE_C:
					return fetch(internal.getC(), sequence.crop());
				case DNASequence.BASE_G:
					return fetch(internal.getG(), sequence.crop());
				case DNASequence.BASE_T:
					return fetch(internal.getT(), sequence.crop());
				case DNASequence.TERMINATOR:
					return fetch(internal.get$(), sequence.crop());
				default:
					System.out.println("Error: getsize");
					return loadNode(FLYWEIGHT);
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
		TrieNode rt = loadNode(root);
		rt = remove(rt, sequence);
		root = saveNode(rt);
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
		if (node.isFlyweight())
		{

			return node;
		}
		//sequence could be here
		else if (node.isLeaf())
		{
			LeafNode leaf = (LeafNode) node;
			String seq = retrieve(leaf.getHandle(), leaf.getLiteralLength());
			//sequence is here; set node to FLYWEIGHT and return
			if (seq.equals(sequence.getSequence()))
			{
				node = loadNode(FLYWEIGHT);
				size--;

				int s = manager.remove(leaf.getHandle()); //remove from pool
				DNAFile.output.println("\nDeleted old record \"" + sequence + "\" "
						+ "of " + (s + 2) + " bytes (" + leaf.getLiteralLength() + " characters)"
						+ " from position " + leaf.getHandle().getAddress());
				return node;
			}
		}
		//InternalNode; recurse through tree
		else
		{
			InternalNode internal = (InternalNode) node;
			switch (sequence.front())
			{
				// change from node.x to node
				case DNASequence.BASE_A:
					//System.out.println("went to A");
					internal.setA(remove(internal.getA(), sequence.crop()));
					break;
				case DNASequence.BASE_C:
					//System.out.println("went to C");
					internal.setC(remove(internal.getC(), sequence.crop()));
					break;
				case DNASequence.BASE_G:
					//System.out.println("went to G");
					internal.setG(remove(internal.getG(), sequence.crop()));
					break;
				case DNASequence.BASE_T:
					//System.out.println("went to T");
					internal.setT(remove(internal.getT(), sequence.crop()));
					break;
				case DNASequence.TERMINATOR:
					//System.out.println("went to $");
					internal.set$(remove(internal.get$(), sequence.crop()));
					break;
				default:
					break;
			}
			//These if blocks determine if collpasing should occur. Nodes should
			//collapse iff there is only one LeafNode linked to node. If there
			//is more than one leaf, no collpasing occurs.
			if (internal.getA().isLeaf() && internal.getC().isFlyweight() && internal.getG().isFlyweight()
					&& internal.getT().isFlyweight() && internal.get$().isFlyweight())
			{
				node = internal.getA();
			}
			else if (internal.getC().isLeaf() && internal.getA().isFlyweight() && internal.getG().isFlyweight()
					&& internal.getT().isFlyweight() && internal.get$().isFlyweight())
			{
				node = internal.getC();
			}
			else if (internal.getG().isLeaf() && internal.getA().isFlyweight() && internal.getC().isFlyweight()
					&& internal.getT().isFlyweight() && internal.get$().isFlyweight())
			{
				node = internal.getG();
			}
			else if (internal.getT().isLeaf() && internal.getA().isFlyweight() && internal.getC().isFlyweight()
					&& internal.getG().isFlyweight() && internal.get$().isFlyweight())
			{
				node = internal.getT();
			}
			else if (internal.get$().isLeaf() && internal.getA().isFlyweight() && internal.getC().isFlyweight()
					&& internal.getG().isFlyweight() && internal.getT().isFlyweight())
			{
				node = internal.get$();
			}
		}
		return node;
	}

	/**
	 * Inserts {@code h} into this tree by using {@code sequence} to determine
	 * where in the tree {@code h} must be placed in order to preserve Trie
	 * properties.
	 * <p/>
	 * @param sequence the {@code DNASequence} to use to map
	 */
	public void insert(DNASequence sequence)
	{
		//for easier insertion, append a $ to the end of the sequence to easily
		//identify sequence termination
		sequence.terminate();
		byte[] bytes = DNACodec.encode(sequence);
		//if bytes is null sequence did not contain any of A, C, G, or T
		if (bytes != null)
		{
			MemHandle newHandle = manager.insert(bytes);
			//error inserting into the pool
			if (newHandle.getAddress() < 0)
			{
				String display = sequence.getSequence() + "\"";
				if (display.length() > DEFAULT_STRING_CROP_LENGTH)
				{
					display = display.substring(0, 41) + "...\" ("
							+ display.length() + " characters)";
				}
				DNAFile.output.println("\nUnable to insert record \""
						+ display + " (insufficient free space)");
			}
			//good to go!
			else
			{
				TrieNode rt = codec.decode(manager.get(root));
				rt = insert(rt, newHandle, sequence, sequence.literalLength(), 0);
				root = manager.insert(codec.encode(rt));
				DNAFile.output.println("\nSuccessfully inserted new "
						+ "record \"" + sequence + "\" of "
						+ (bytes.length + 2) + " bytes ("
						+ sequence.literalLength() + " characters) starting "
						+ "at position " + newHandle.getAddress());

			}
		}
		//invalid DNA sequence (no A, C, G, or T)
		else
		{
			String display = sequence + "\"";
			if (display.length() > DEFAULT_STRING_CROP_LENGTH)
			{
				display = display.substring(0, 41) + "...\" ("
						+ display.length() + " characters)";
			}
			DNAFile.output.println("\nUnable to insert record \"" + display
					+ " (sequence does not contain any valid DNA "
					+ "characters)");
		}
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
		if (node.isFlyweight())
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
			DNASequence seq = new DNASequence(retrieve(leaf.getHandle(), leaf.getLiteralLength()));
			if (seq.equals(sequence))
			{
				DNAFile.output.println("INSERT: Cannot insert duplicate record \""
						+ sequence + "\".");
			}
			seq.terminate();
			seq.cropAt(depth);
			//System.out.println(seq + "; " + seq.getCurrent() + ": " + sequence + "; " + sequence.getCurrent() + " - " + depth);
			node = insert(node, h, sequence, length, depth);
			node = insert(node, leaf.getHandle(), seq, leaf.getLiteralLength(), depth);
		}
		//InternalNode
		else
		{
			InternalNode internal = (InternalNode) node;
			//System.out.println("internal " + node);
			switch (sequence.front())
			{
				case DNASequence.BASE_A:
					//System.out.println("went to A");
					TrieNode ATree = internal.getA();
					ATree = insert(ATree, h, sequence.crop(), length, depth + 1);
					internal.setA(ATree);
					break;
				case DNASequence.BASE_C:
					//System.out.println("went to C");
					TrieNode CTree = internal.getC();
					CTree = insert(CTree, h, sequence.crop(), length, depth + 1);
					internal.setC(CTree);
					break;
				case DNASequence.BASE_G:
					//System.out.println("went to G");
					TrieNode GTree = internal.getG();
					GTree = insert(GTree, h, sequence.crop(), length, depth + 1);
					internal.setG(GTree);
					break;
				case DNASequence.BASE_T:
					//System.out.println("went to T");
					TrieNode TTree = internal.getT();
					TTree = insert(TTree, h, sequence.crop(), length, depth + 1);
					internal.setT(TTree);
					break;
				case DNASequence.TERMINATOR:
					//System.out.println("went to $");
					TrieNode $Tree = internal.get$();
					$Tree = insert($Tree, h, sequence.crop(), length, depth + 1);
					internal.set$($Tree);
					break;
				default:
					DNAFile.output.println("Error during insert");
					break;
			}
		}
		return node;
	}

	/**
	 * Returns a String representation of this {@code trie}. Depending upon the
	 * value of {@code request}, various information can be appended to the
	 * basic String representation, including {@code length} for printing the
	 * number of characters in each discovered {@link DNASequence} and
	 * statistics for each occurring character in each discovered
	 * )@link DNASequence}.
	 * <p/>
	 * @param request the type of print desired, either
	 *                   {@link #JUST_DO_IT_SON}, {@link #BY_LENGTH}, or
	 *                   {@link #BY_STATS}
	 * <p/>
	 * @return a String representation of this {@code trie} based on the value
	 *            of {@code request}
	 */
	public String printTrie(int request)
	{
		if (request == JUST_DO_IT_SON)
		{
			return printTrie(loadNode(root), 0);
		}
		else if (request == BY_LENGTH)
		{
			return printTrieByLength(loadNode(root), 0);
		}
		else if (request == BY_STATS)
		{
			return printTrieByStats(loadNode(root), 0);
		}
		return "";
	}

	/**
	 * Prints the entire contents of {@code tree} with no extra information.
	 * <p/>
	 * @param node  the {@link TrieNode} to examine
	 * @param depth the current depth within the tree; used to determine how
	 *                 many spaces are needed
	 * <p/>
	 * @return a String representation of {@code tree}
	 */
	private String printTrie(TrieNode node, int depth)
	{
		StringBuilder builder = new StringBuilder();
		int numSpaces = 2 * depth;
		//append spaces based on tree depth
		for (int i = 0; i < numSpaces; i++)
		{
			builder.append(" ");
		}
		//FLYWEIGHTS are printed as E
		if (node.isFlyweight())
		{
			builder.append("E\n");
		}
		//LeafNodes are printed as their sequence
		else if (node.isLeaf())
		{
			LeafNode leaf = (LeafNode) node;
			builder.append(retrieve(leaf.getHandle(),
					leaf.getLiteralLength())).append("\n");
		}
		//InternalNodes are printed as I and have their nodes examined via DFS
		else
		{
			InternalNode internal = (InternalNode) node;
			builder.append("I\n").append(printTrie(internal.getA(), depth + 1)).append(
					printTrie(internal.getC(), depth + 1)).append(
					printTrie(internal.getG(), depth + 1)).append(
					printTrie(internal.getT(), depth + 1)).append(
					printTrie(internal.get$(), depth + 1));
		}
		return builder.toString();
	}

	/**
	 * Prints the entire contents of {@code tree} with information about the
	 * length of each {@link DNASequence}.
	 * <p/>
	 * @param node  the {@link TrieNode} to examine
	 * @param depth the current depth within the tree; used to determine how
	 *                 many spaces are needed
	 * <p/>
	 * @return a String representation of {@code tree} with length information
	 */
	private String printTrieByLength(TrieNode node, int depth)
	{
		StringBuilder builder = new StringBuilder();
		int numSpaces = 2 * depth;
		//append spaces based on tree depth
		for (int i = 0; i < numSpaces; i++)
		{
			builder.append(" ");
		}
		//FLYWEIGHTS are printed as E
		if (node.isFlyweight())
		{
			builder.append("E\n");
		}
		//LeafNodes are printed as their sequence
		else if (node.isLeaf())
		{
			LeafNode leaf = (LeafNode) node;
			int literal = leaf.getLiteralLength();
			builder.append(retrieve(leaf.getHandle(), literal)).append(": length ").append(literal).append(" \n");
		}
		//InternalNodes are printed as I and have their nodes examined via DFS
		else
		{
			InternalNode internal = (InternalNode) node;
			builder.append("I\n").append(printTrieByLength(internal.getA(), depth + 1)).append(
					printTrieByLength(internal.getC(), depth + 1)).append(
					printTrieByLength(internal.getG(), depth + 1)).append(
					printTrieByLength(internal.getT(), depth + 1)).append(
					printTrieByLength(internal.get$(), depth + 1));
		}
		return builder.toString();
	}

	/**
	 * Prints the entire contents of {@code tree} with statistics of each
	 * character occurring in the {@link DNASequence}.
	 * <p/>
	 * @param node  the {@link TrieNode} to examine
	 * @param depth the current depth within the tree; used to determine how
	 *                 many spaces are needed
	 * <p/>
	 * @return a String representation of {@code tree} with statistics
	 */
	private String printTrieByStats(TrieNode node, int depth)
	{
		StringBuilder builder = new StringBuilder();
		int numSpaces = 2 * depth;
		//append spaces based on tree depth
		for (int i = 0; i < numSpaces; i++)
		{
			builder.append(" ");
		}
		//FLYWEIGHTS are printed as E
		if (node.isFlyweight())
		{
			builder.append("E\n");
		}
		//LeafNodes are printed as their sequence
		else if (node.isLeaf())
		{
			LeafNode leaf = (LeafNode) node;
			DNASequence seq = new DNASequence(retrieve(leaf.getHandle(), leaf.getLiteralLength()));
			builder.append(seq.getSequence()).append(" ").
					append(seq.getStats()).append("\n");
		}
		//InternalNodes are printed as I and have their nodes examined via DFS
		else
		{
			InternalNode internal = (InternalNode) node;
			builder.append("I\n").append(printTrieByStats(internal.getA(), depth + 1)).append(
					printTrieByStats(internal.getC(), depth + 1)).append(
					printTrieByStats(internal.getG(), depth + 1)).append(
					printTrieByStats(internal.getT(), depth + 1)).append(
					printTrieByStats(internal.get$(), depth + 1));
		}
		return builder.toString();
	}

	/**
	 * Retrieves a String representing a {@code DNASequence} from memory.
	 * <p/>
	 * @param h      the {@link MemHandle} referencing the sequence in the pool
	 * @param length the literal (true) length of the expected sequence
	 * <p/>
	 * @return the sequence
	 */
	public String retrieve(MemHandle h, int length)
	{
		byte[] got = manager.get(h);
		//if got is null, no sequence exists in memory referenced by h
		if (got != null)
		{
			String seq = DNACodec.decode(got).getSequence();
			return verifyDecode(seq, length);
		}
		return "";
	}

	/**
	 * Verifies the decoding operation by ensuring that A's and C's are inserted
	 * properly. Because the encoding operation trims leading zero's, this
	 * operation restores leading zeros up to the nearest full byte then only
	 * examines the bits that are relevant ({@code size} determines this).
	 * <p/>
	 * @param s    the partially decoded binary String
	 * @param size the expected size (in String characters) of the String
	 * <p/>
	 * @return the fully decoded String
	 */
	private String verifyDecode(String s, int size)
	{
		String keep = "";
		//determines how many zeros need to be restored
		int dif = size * 2 - s.length();
		for (int i = 0; i < dif; i++)
		{
			keep += "0";
		}
		keep += s;
		String ret = "";
		for (int i = 0; i < keep.length(); i += 2)
		{
			int next = i + 2;
			String curr = keep.substring(i, next);
			if (curr.equals("00"))
			{
				ret += "A";
			}
			if (curr.equals("01"))
			{
				ret += "C";
			}
			if (curr.equals("10"))
			{
				ret += "G";
			}
			if (curr.equals("11"))
			{
				ret += "T";
			}
		}
		return ret;
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

	private TrieNode loadNode(MemHandle h)
	{
		return codec.decode(manager.get(h));
	}

	private MemHandle saveNode(TrieNode node)
	{
		return manager.insert(codec.encode(node));
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
		if (node.isFlyweight())
		{
			return 1;
		}
		//add match
		else if (node.isLeaf())
		{
			LeafNode leaf = (LeafNode) node;
			DNASequence seq = new DNASequence(retrieve(leaf.getHandle(), leaf.getLiteralLength()));
			c.add(seq.getSequence());
			return 1;
		}
		else
		{
			InternalNode internal = (InternalNode) node;
			return 1 + loadPrefixes(internal.getA(), c) + loadPrefixes(internal.getC(), c)
					+ loadPrefixes(internal.getG(), c) + loadPrefixes(internal.getT(), c)
					+ loadPrefixes(internal.get$(), c);
		}
	}

	/**
	 * The {@code TrieNode} abstract class marks an implementing subclass a node
	 * to operate upon in a {@link DNATrie}. Any class implementing this super
	 * class must be able to distinguish leaf nodes.
	 * <p/>
	 * To avoid wasting precious space with useless empty nodes, or to preclude
	 * forcing {@code null} checks, any node that should be empty or
	 * {@code null} is instead directed to {@link #FLYWEIGHT}. This means one,
	 * easy check can be made to determine if a node is empty or not.
	 * <p/>
	 * @author orionf22
	 * @author rinaldi1
	 */
	public interface TrieNode
	{

		/**
		 * Denotes this {@code TrieNode} as a leaf node.
		 * <p/>
		 * @return {@code true} if this {@code TrieNode} is a leaf node,
		 *               {@code false} otherwise
		 */
		public abstract boolean isLeaf();

		/**
		 * Denotes this {@code TrieNode} as the {@link DNATrie#FLYWEIGHT}.
		 * <p/>
		 * @return {@code true} if this {@code TrieNode} is the
		 *               {@link DNATrie#FLYWEIGHT}, {@code false} otherwise
		 */
		public abstract boolean isFlyweight();
	}

	/**
	 * Flyweight design; all empty (and thus {@code null}) nodes point to a
	 * single {@code FLYWEIGHT} node, {@link DNATrie#FLYWEIGHT}.
	 */
	private class FLYWEIGHT
			implements TrieNode
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

	/**
	 * {@code InternalNode} objects represent an internal node within a tree
	 * structure. They are not {@link LeafNode} objects, nor are they
	 * {@link TrieNode.FLYWEIGHT} objects. Instead, {@code InternalNode} objects
	 * have five children that could be any of the three types of
	 * {@link TrieNode}.
	 * <p/>
	 * @author orionf22
	 * @author rinaldi1
	 */
	private class InternalNode
			implements TrieNode
	{

		/**
		 * The A subtree.
		 */
		private MemHandle A = FLYWEIGHT;
		/**
		 * The C subtree.
		 */
		private MemHandle C = FLYWEIGHT;
		/**
		 * The G subtree.
		 */
		private MemHandle G = FLYWEIGHT;
		/**
		 * The T subtree.
		 */
		private MemHandle T = FLYWEIGHT;
		/**
		 * The $ subtree (sequence terminator).
		 */
		private MemHandle $ = FLYWEIGHT;

		public TrieNode getA()
		{
			return codec.decode(manager.get(A));
		}

		public void setA(TrieNode node)
		{
			this.A = manager.insert(codec.encode(node));
		}

		public TrieNode getC()
		{
			return codec.decode(manager.get(C));
		}

		public void setC(TrieNode node)
		{
			this.C = manager.insert(codec.encode(node));
		}

		public TrieNode getG()
		{
			return codec.decode(manager.get(G));
		}

		public void setG(TrieNode node)
		{
			this.G = manager.insert(codec.encode(node));
		}

		public TrieNode getT()
		{
			return codec.decode(manager.get(T));
		}

		public void setT(TrieNode node)
		{
			this.T = manager.insert(codec.encode(node));
		}

		public TrieNode get$()
		{
			return codec.decode(manager.get($));
		}

		public void set$(TrieNode node)
		{
			this.$ = manager.insert(codec.encode(node));
		}

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

	/**
	 * {@code LeafNode} objects store a {@link MemHandle} and integer value.
	 * Both are used to retrieve a stored piece of information from a memory
	 * pool.
	 * <p/>
	 * @author orionf22
	 * @author rinaldi1
	 */
	private class LeafNode
			implements TrieNode
	{

		/**
		 * The stored {@link MemHandle}.
		 */
		private MemHandle handle;
		/**
		 * The true length of the stored information referenced by
		 * {@code handle}.
		 */
		private int length;

		/**
		 * Constructs a new {@code LeafNode} from {2code h} and {@code l}.
		 * <p/>
		 * @param h the {@link MemHandle} to use
		 * @param l the true length of the stored information
		 */
		private LeafNode(MemHandle h, int l)
		{
			this.handle = h;
			this.length = l;
		}

		/**
		 * Returns the stored {@link MemHandle}.
		 * <p/>
		 * @return the {@link MemHandle}
		 */
		public MemHandle getHandle()
		{
			return this.handle;
		}

		/**
		 * Returns the stored true length of information.
		 * <p/>
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

	/**
	 * The {@code NodeCodec} class encodes and decodes {@link TrieNode} objects.
	 * <p/>
	 * @author orionf22
	 * @author rinaldi1
	 */
	public class NodeCodec
			implements Codec<DNATrie.TrieNode>
	{

		@Override
		public DNATrie.TrieNode decode(byte[] bytes)
		{
			TrieNode ret = null;
			ByteBuffer buff = ByteBuffer.allocate(bytes.length);
			buff.put(bytes);
			byte first = buff.get(0);
			//InternalNode
			if (first == 0)
			{
				ret = new InternalNode();
				
			}
			//LeafNode
			else if (first == 1)
			{
			}
			else if (first == -2)
			{
			}
			return ret;
		}

		@Override
		public byte[] encode(DNATrie.TrieNode stuff)
		{
			byte[] ret;
			ByteBuffer buff;
			if (stuff.isLeaf())
			{
				LeafNode leaf = (LeafNode) stuff;
				ret = new byte[7];
				buff = ByteBuffer.allocate(4);
				ret[0] = 1;
				int length = leaf.getLiteralLength();
				buff.putShort((short) length);
				ret[1] = buff.get(0);
				ret[2] = buff.get(1);
				buff.putInt(0, leaf.getHandle().getAddress());
				ret[3] = buff.get(0);
				ret[4] = buff.get(1);
				ret[5] = buff.get(2);
				ret[6] = buff.get(3);
			}
			else if (stuff.isFlyweight())
			{
				ret = new byte[1];
				ret[0] = -2;
			}
			else
			{
				InternalNode internal = (InternalNode) stuff;
				ret = new byte[21];
				buff = ByteBuffer.allocate(4);
				ret[0] = 0;
				buff.putInt(internal.A.getAddress());
				ret[1] = buff.get(0);
				ret[2] = buff.get(1);
				ret[3] = buff.get(2);
				ret[4] = buff.get(3);
				buff.putInt(0, internal.C.getAddress());
				ret[5] = buff.get(0);
				ret[6] = buff.get(1);
				ret[7] = buff.get(2);
				ret[8] = buff.get(3);
				buff.putInt(0, internal.G.getAddress());
				ret[9] = buff.get(0);
				ret[10] = buff.get(1);
				ret[11] = buff.get(2);
				ret[12] = buff.get(3);
				buff.putInt(0, internal.T.getAddress());
				ret[13] = buff.get(0);
				ret[14] = buff.get(1);
				ret[15] = buff.get(2);
				ret[16] = buff.get(3);
				buff.putInt(0, internal.$.getAddress());
				ret[17] = buff.get(0);
				ret[18] = buff.get(1);
				ret[19] = buff.get(2);
				ret[20] = buff.get(3);
			}
			return ret;
		}
	}
}
