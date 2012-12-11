
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;

/**
 * {@code DNATrie} objects are 5-way branching trees that store
 * {@link MemHandle} and integer objects but are sorted by {@link DNASequence}
 * objects. Each node can branch into five children: <ul><li>A</li> <li>C</li>
 * <li>G</li> <li>T</li> <li>$</li></ul>
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
 * A {@link MemManager} is used to store any {@link TrieNode} not currently in
 * use in some function. This reduces the program's memory footprint at the cost
 * of some speed by storing nodes on disk. Stored {@link DNASequence} objects
 * are also stored on disk.
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
		this.DNACodec = new DNACodec();
		FLYWEIGHT = saveNode(new FLYWEIGHT());
		root = FLYWEIGHT;
	}

	/**
	 * Returns this tree's {@link MemManager}.
	 * <p/>
	 * @return the {@link MemManager}
	 */
	public MemManager getManager()
	{
		return this.manager;
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
		if (size == 0)
		{
			return false;
		}
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
	 * properties. Duplicate sequences are not permitted, so the sequence if
	 * first fetched from the Trie via {@link #fetch(DNASequence)}, which
	 * returns a {@code boolean}. If this value is {@code true}, the sequence is
	 * already in the Trie so inserting is not permitted. If the sequence is not
	 * a duplicate, it is encoded and sent to {@link #manager manager} for
	 * storage.
	 * <p/>
	 * @param sequence the {@code DNASequence} to insert
	 */
	public void insert(DNASequence sequence)
	{
		//for easier insertion, append a $ to the end of the sequence to easily
		//identify sequence termination
		sequence.terminate();
		//determine if this sequence is already in the tree
		boolean isDuplicate = fetch(sequence);
		if (isDuplicate)
		{
			DNAFile.output.println("INSERT: Cannot insert duplicate record \""
					+ sequence + "\".");
			return;
		}
		//restore and terminate again! failure to do so could cause errors 
		//further in insertion
		sequence.restore();
		sequence.terminate();
		byte[] bytes = DNACodec.encode(sequence);
		//if bytes is null sequence did not contain any of A, C, G, or T
		if (bytes != null)
		{
			MemHandle newHandle = manager.insert(bytes);
			//error inserting into the pool or no space
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
				//retrieve the root node from disk and insert on it
				TrieNode rt = loadNode(root);
				rt = insert(rt, newHandle, sequence, sequence.literalLength(), 0);
				//return root node to disk, updating the root handle
				root = saveNode(rt);
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
	 * operates on recursion; base case is when {@link #FLYWEIGHT FLYWEIGHT} is
	 * reached. This means this is the correct location for the inserting
	 * values.
	 * <p/>
	 * If an {@link InternalNode} is reached, branches must be examined based on
	 * the current leading character in {@code sequence}. If a {@link LeafNode}
	 * is reached, its existing values must be relocated and the leaf split
	 * (converting it into a new {@link InternalNode}). To accomplish this, the
	 * existing {@link MemHandle} and literal length are used to query the
	 * {@link MemManager} for the actual sequence stored here. This sequence is
	 * then cropped based on the depth within the tree so it can be reinserted
	 * at the current level.
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
			size++;
			node = new LeafNode(h, length);
		}
		//split node as new InternalNode. Insert existing info and continue 
		//inserting original info
		else if (node.isLeaf())
		{
			LeafNode leaf = (LeafNode) node;
			node = new InternalNode();
			DNASequence seq = new DNASequence(
					retrieve(leaf.getHandle(), leaf.getLiteralLength()));
			seq.terminate();
			seq.cropAt(depth);
			node = insert(node, h, sequence, length, depth);
			node = insert(node, leaf.getHandle(), seq, leaf.getLiteralLength(), depth);
		}
		//InternalNode
		else
		{
			//cast as internal in order to get and set children
			InternalNode internal = (InternalNode) node;
			//when a switch is determined, the appropriate child handle is 
			//acquired from the MemManager, loaded as a node, operated upon, and
			//returned to the MemManager
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
		String ret = "";
		TrieNode rt = loadNode(root);
		if (request == JUST_DO_IT_SON)
		{
			ret = printTrie(rt, 0);
		}
		else if (request == BY_LENGTH)
		{
			ret = printTrieByLength(rt, 0);
		}
		else if (request == BY_STATS)
		{
			ret = printTrieByStats(rt, 0);
		}
		root = saveNode(rt);
		return ret + "\nBufferPool IDs:\n" + manager.getBlockIDs();
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
		//System.out.println(Arrays.toString(got) + "; " + h.getAddress());
		//if got is null, no sequence exists in memory referenced by h
		if (got != null)
		{
			String seq = DNACodec.decode(got).getSequence();
			return completeDecode(seq, length);
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
	private String completeDecode(String s, int size)
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

	/**
	 * Loads a {@link TrieNode} from the {@link MemManager} given a
	 * {@link MemHandle} {@code h}.
	 * <p/>
	 * @param h the location in disk where the node is stored
	 * <p/>
	 * @return a complete {@Link TrieNode} object
	 */
	private TrieNode loadNode(MemHandle h)
	{
		//System.out.println("\t\tinserting at: " + h.getAddress());
		TrieNode ret = codec.decode(manager.get(h));
		return ret;
	}

	/**
	 * Returns a {@link TrieNode} {@code node} to disk via the
	 * {@link MemManager}.
	 * <p/>
	 * @param node the {@link TrieNode} to return to disk
	 * <p/>
	 * @return the updated {@link MemHandle} denoting the new location of
	 *            {@code node} on disk
	 */
	private MemHandle saveNode(TrieNode node)
	{
		byte[] bytes = codec.encode(node);
		MemHandle ret = manager.insert(bytes);
//		System.out.println("Saved " + node.getClass()
//				+ " (" + (bytes.length + 2) + " bytes) starting at position " + ret.getAddress());
//		System.out.println(manager.getFreeBlocks());
		return ret;
	}

	/**
	 * Returns the number of nodes visited during a search, typically a prefix
	 * search. Any and all matches are added to {@code c}.
	 * <p/>
	 * This method does not need to search for a match. {@code node} is
	 * eventually expected to be an "umbrella" node, meaning it is the first
	 * node in a tree that contains a prefix match. Because of Trie properties,
	 * any and all nodes below {@code node} must also contain the prefix, so a
	 * DFS is used to add each sequence to {@code c}.
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
	 * The {@code TrieNode} interface represents a basic node in a
	 * {@link DNATrie}. Any class implementing this super class must be able to
	 * distinguish between leaf, internal, and flyweight nodes.
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
		public String toString()
		{
			return "FLYWEIGHT";
		}

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
	 * {@link TrieNode} objects. These children are {@link MemHandle} objects
	 * that identify a location within the {@link MemManager} where an actual
	 * {@link TrieNode} can be acquired via
	 * {@link DNATrie#loadNode(MemHandle) loadNode(MemHandle)}.
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

		@Override
		public String toString()
		{
			return "A: " + this.getA() + " C: " + this.getC() + " G: "
					+ this.getG() + " T: " + this.getT() + " $: " + this.get$();
		}

		/**
		 * Acquires this node's A subtree from the {@link MemManager}.
		 * <p/>
		 * @return the A subtree
		 */
		public TrieNode getA()
		{
			return loadNode(A);
		}

		/**
		 * Sets this node's A subtree to {@code node} by inserting it into the
		 * {@link MemManager}.
		 * <p/>
		 * @param node the new A subtree
		 */
		public void setA(TrieNode node)
		{
			if (!A.equals(FLYWEIGHT))
			{
				manager.remove(A);
			}
			this.A = saveNode(node);
		}

		/**
		 * Sets this node's A subtree to {@code a}.
		 * <p/>
		 * @param a the new A subtree {@link MemHandle}
		 */
		public void setA(MemHandle a)
		{
			this.A = a;
		}

		/**
		 * Acquires this node's C subtree from the {@link MemManager}.
		 * <p/>
		 * @return the C subtree
		 */
		public TrieNode getC()
		{
			return loadNode(C);
		}

		/**
		 * Sets this node's C subtree to {@code node} by inserting it into the
		 * {@link MemManager}.
		 * <p/>
		 * @param node the new C subtree
		 */
		public void setC(TrieNode node)
		{
			if (!C.equals(FLYWEIGHT))
			{
				manager.remove(C);
			}
			this.C = saveNode(node);
		}

		/**
		 * Sets this node's C subtree to {@code c}.
		 * <p/>
		 * @param c the new C subtree {@link MemHandle}
		 */
		public void setC(MemHandle c)
		{
			this.C = c;
		}

		/**
		 * Acquires this node's G subtree from the {@link MemManager}.
		 * <p/>
		 * @return the G subtree
		 */
		public TrieNode getG()
		{
			return loadNode(G);
		}

		/**
		 * Sets this node's G subtree to {@code node} by inserting it into the
		 * {@link MemManager}.
		 * <p/>
		 * @param node the new G subtree
		 */
		public void setG(TrieNode node)
		{
			if (!G.equals(FLYWEIGHT))
			{
				manager.remove(G);
			}
			this.G = saveNode(node);
		}

		/**
		 * Sets this node's G subtree to {@code g}.
		 * <p/>
		 * @param g the new G subtree {@link MemHandle}
		 */
		public void setG(MemHandle g)
		{
			this.G = g;
		}

		/**
		 * Acquires this node's T subtree from the {@link MemManager}.
		 * <p/>
		 * @return the T subtree
		 */
		public TrieNode getT()
		{
			return loadNode(T);
		}

		/**
		 * Sets this node's T subtree to {@code node} by inserting it into the
		 * {@link MemManager}.
		 * <p/>
		 * @param node the new T subtree
		 */
		public void setT(TrieNode node)
		{
			if (!T.equals(FLYWEIGHT))
			{
				manager.remove(T);
			}
			this.T = saveNode(node);
		}

		/**
		 * Sets this node's T subtree to {@code t}.
		 * <p/>
		 * @param t the new T subtree {@link MemHandle}
		 */
		public void setT(MemHandle t)
		{
			this.T = t;
		}

		/**
		 * Acquires this node's $ subtree from the {@link MemManager}.
		 * <p/>
		 * @return the $ subtree
		 */
		public TrieNode get$()
		{
			return loadNode($);
		}

		/**
		 * Sets this node's $ subtree to {@code node} by inserting it into the
		 * {@link MemManager}.
		 * <p/>
		 * @param node the new $ subtree
		 */
		public void set$(TrieNode node)
		{
			if (!$.equals(FLYWEIGHT))
			{
				manager.remove($);
			}
			this.$ = saveNode(node);
		}

		/**
		 * Sets this node's $ subtree to {@code $}.
		 * <p/>
		 * @param $ the new $ subtree {@link MemHandle}
		 */
		public void set$(MemHandle $)
		{
			this.$ = $;
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

		@Override
		public String toString()
		{
			return "Leaf: " + this.getHandle() + "; " + this.getLiteralLength();
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
	 * A {@link ByteBuffer} is used to convert information to and from bytes in
	 * a simple fashion.
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
			if (bytes == null || bytes.length == 0)
			{
				System.out.println("null or zero-length decode request");
				return null;
			}
			TrieNode ret = null;
			//go ahead and allocate enough space for the entire byte array
			ByteBuffer buff = ByteBuffer.allocate(bytes.length);
			//put them into buff
			buff.put(bytes);
			//the first byte identifies the type of node; this represents the 
			//isLeaf flag
			byte first = buff.get(0);
			//InternalNode
			if (first == 0)
			{
				InternalNode internal = new InternalNode();
				//each child is four bytes in length
				int a = buff.getInt(1);
				int c = buff.getInt(5);
				int g = buff.getInt(9);
				int t = buff.getInt(13);
				int $ = buff.getInt(17);
				internal.setA(new MemHandle(a));
				internal.setC(new MemHandle(c));
				internal.setG(new MemHandle(g));
				internal.setT(new MemHandle(t));
				internal.set$(new MemHandle($));
				ret = internal;
			}
			//LeafNode
			else if (first == 1)
			{
				int length = buff.getShort(1);
				int address = buff.getInt(3);
				ret = new LeafNode(new MemHandle(address), length);
			}
			//FLYWEIGHT
			else if (first == -2)
			{
				ret = new FLYWEIGHT();
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
				//first byte is the isLeaf flag, true for LeafNodes
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
				//use -2 as arbitrary value for FLYWEIGHTS
				ret[0] = -2;
			}
			else
			{
				InternalNode internal = (InternalNode) stuff;
				ret = new byte[21];
				//while a larger allocation could be specified, this buffer will
				//only be dealing with 4 bytes per subtree address, so a few
				//bytes can be saved by instead reinserting in the buffer at 
				//index 0
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
