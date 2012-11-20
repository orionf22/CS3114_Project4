
import java.util.ArrayList;

/**
 * The {@code Controller} class manages memory requests and allocation. It uses
 * a {@link DNATrie} to maintain a proper database of stored and available
 * records (data) and delegates further memory management to a
 * {@link MemManager}.
 * <p/>
 * Data reading/writing (aka encoding/decoding) is handled via a provided
 * {@link Codec}. Any class properly implementing {@link Codec} can be supplied
 * to this {@code Controller} for the purposes of translating given information
 * into bytes and back again.
 * <p/>
 * In the step-by-step process of handling memory record requests: <ul><li>The
 * {@code Controller} queries its {@link MemManager} to see if it can honor a
 * new storage request.</li> <li>If space is available, the {@link MemManager}
 * will then request enough free space from its {@link FreeBlockList}.</li><li>
 * If there is enough free space, it is removed from the {@link FreeBlockList}
 * and a new block of memory is passed to the {@link MemoryPool}.</li><li>The
 * memory pool uses the newly allocated space to store the request and the
 * {@link MemManager} passes up a {@link MemHandle} that can be used by
 * higher-level classes to retrieve information stored in the
 * {@link MemoryPool}.</li><li>The {@link DNATrie} receives the
 * {@link MemHandle} and an integer representing the true length of the stored
 * data. These objects are stored in {@link LeafNode} objects for later
 * retrieval.</li> </ul>
 * <p/>
 * @author rinaldi1
 * @author orionf22
 */
public class Controller
{

	/**
	 * The {@link MemManager} owned by this {@code Controller}.
	 */
	private MemManager manager;
	/**
	 * The {@link DNATrie} owned by this {@code Controller}.
	 */
	private DNATrie tree;
	/**
	 * The {@link BufferPool} used by this {@link Controller}.
	 */
	private BufferPool bufferPool;
	/**
	 * The {@link Codec} used to translate information to and from bytes and a
	 * given format.
	 */
	private Codec codec;
	/**
	 * Default length of a printed record before remaining characters will be
	 * cropped off. This improves output readability.
	 */
	private static final int DEFAULT_STRING_CROP_LENGTH = 40;

	/**
	 * Constructs a new {@code Controller} with references to proper
	 * {@link MemManager} and {@link DNATrie} objects.
	 * <p/>
	 * @param m the {@link MemManager} to use
	 * @param t the {@link DNATrie} to use
	 */
	public Controller(MemManager m, DNATrie t)
	{
		this.manager = m;
		this.tree = t;
	}

	/**
	 * Sets the {@link Codec} to use during data translation.
	 * <p/>
	 * @param c the {@link Codec} to use
	 */
	public void setCodec(Codec c)
	{
		this.codec = c;
	}

	/**
	 * Attempts to insert a new record into {@code manager} and {@code tree}.
	 * The {@link MemManager} is first queried for available byte space; if
	 * there exists room, command is delegated down to the {@link MemManager}.
	 * See that class for more information.
	 * <p/>
	 * @param c the {@link InsertCommand} to follow
	 */
	public void insertRecord(InsertCommand c)
	{
		//Attempt to find this sequence first. If it is found, this new sequence
		//is a duplicate and is not to be inserted
		TrieNode curr = tree.fetch(new DNASequence(c.getInfo()));
		//Something exists here; duplicates are forbidden
		if (curr.isLeaf())
		{
			DNAFile.output.println("INSERT: Cannot insert duplicate record \""
					+ c.getInfo() + "\".");
		}
		//This is a unique sequence, attempt to insert
		else
		{
			byte[] bytes = codec.encode(c.getInfo());
			//if bytes is null sequence did not contain any of A, C, G, or T
			if (bytes != null)
			{
				MemHandle newHandle = manager.insert(bytes);
				//error inserting into the pool
				if (newHandle.getAddress() < 0)
				{
					String display = c.getInfo() + "\"";
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
					tree.insert(newHandle, new DNASequence(c.getInfo()));
					DNAFile.output.println("\nSuccessfully inserted new "
							+ "record \"" + c.getInfo() + "\" of "
							+ (bytes.length + 2) + " bytes ("
							+ c.getInfo().length() + " characters) starting "
							+ "at position " + newHandle.getAddress());

				}
			}
			//invalid DNA sequence (no A, C, G, or T)
			else
			{
				String display = c.getInfo() + "\"";
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
	}

	/**
	 * Attempts to remove a record (data) from memory. If the {@link DNATrie}
	 * declares nothing exists in memory at the location specified by {@code c},
	 * nothing will happen. Otherwise, that record (data) will be removed. To
	 * accomplish this, command is delegated to the {@link MemManager}; see for
	 * more information.
	 * <p/>
	 * @param c the {@link RemoveCommand} to follow
	 */
	public void removeRecord(RemoveCommand c)
	{
		DNASequence sequence = new DNASequence(c.getSequence());
		//ERROR! sometimes this doesn't get what we want...due to incomplete
		//DNATrie.insert method
		TrieNode toRemove = DNATrie.FLYWEIGHT;
		try
		{
			toRemove = tree.fetch(sequence);
			sequence.restore();
		}
		catch (Exception e)
		{
			DNAFile.output.println("Attempted to find \"" + c.getSequence()
					+ "\"; " + e.getClass().getName() + " encountered. Removal "
					+ "failure. Debug: " + e.toString());
		}
		//if we got FLYWEIGHT, either an error occured or the sequence does not 
		//exist
		if (toRemove == DNATrie.FLYWEIGHT)
		{
			DNAFile.output.println("\nUnable to remove sequence \""
					+ c.getSequence() + "\"; sequence not found.");
		}
		//else remove the sequence
		else
		{
			LeafNode leaf = (LeafNode) toRemove;
			int literal = leaf.getLiteralLength();
			MemHandle remove = leaf.getHandle();
			tree.remove(sequence); // Must remove from the tree first
			int size = manager.remove(remove); // Then remove from the pool!
			DNAFile.output.println("\nDeleted old record \"" + sequence + "\" "
					+ "of " + (size + 2) + " bytes (" + literal + " characters)"
					+ " from position " + remove.getAddress());
		}
	}

	/**
	 * Prints the status of managed memory to {@link DNAFile#output}.
	 * <p/>
	 * @param c the {@link PrintCommand} to follow
	 */
	public void print(PrintCommand c)
	{
		int request = c.getRequest();
		//No param was specified; print Trie and freelist
		if (request == DNATrie.JUST_DO_IT_SON)
		{
			DNAFile.output.println(printTrie(tree.getRoot(), 0));
		}
		//print Trie with lenghts info and freelist
		else if (request == DNATrie.BY_LENGTH)
		{
			DNAFile.output.println(printTrieByLength(tree.getRoot(), 0));
		}
		//print Trie with stats info and freelist
		else if (request == DNATrie.BY_STATS)
		{
			DNAFile.output.println(printTrieByStats(tree.getRoot(), 0));
		}
		//also print the free list status
		DNAFile.output.println("\nFreeblock list:\n" + manager.getFreeBlocks()
				+ "\nBuffer Pool:\n" + bufferPool.getBlockIDs());
	}

	/**
	 * Attempts to find a record in {@code tree}. If the provided
	 * {@link DNASequence} contained with {@code c} is terminated with {@code $}
	 * then an explicit match must be found. Otherwise the sequence represents a
	 * prefix to find, so any sequence containing that sequence as a prefix will
	 * be found.
	 * <p/>
	 * @param c the {@link SearchCommand} to follow
	 */
	public void search(SearchCommand c)
	{
		DNASequence sequence = new DNASequence(c.getSequence());
		ArrayList<String> matches = new ArrayList<>();
		int nodesVisited = tree.get(sequence, matches);
		DNAFile.output.println("\nNodes visited: " + nodesVisited);
		//at least one match in the collection
		if (!matches.isEmpty())
		{
			String binary;
			for (int i = 0; i < matches.size(); i++)
			{
				binary = matches.get(i);
				DNAFile.output.println("sequence: " + binary);
			}
		}
		//no matches found
		else
		{
			DNAFile.output.println("sequence \"" + sequence.getSequence()
					+ "\" not found");
		}

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
			String seq = codec.decode(got);
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
		if (node == DNATrie.FLYWEIGHT)
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
			builder.append("I\n").append(printTrie(node.A, depth + 1)).append(
					printTrie(node.C, depth + 1)).append(
					printTrie(node.G, depth + 1)).append(
					printTrie(node.T, depth + 1)).append(
					printTrie(node.$, depth + 1));
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
	 * @return a String representation of {@code tree} with lenght information
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
		if (node == DNATrie.FLYWEIGHT)
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
			builder.append("I\n").append(printTrieByLength(node.A, depth + 1)).append(
					printTrieByLength(node.C, depth + 1)).append(
					printTrieByLength(node.G, depth + 1)).append(
					printTrieByLength(node.T, depth + 1)).append(
					printTrieByLength(node.$, depth + 1));
		}
		return builder.toString();
	}

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
		if (node == DNATrie.FLYWEIGHT)
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
			builder.append("I\n").append(printTrieByStats(node.A, depth + 1)).append(
					printTrieByStats(node.C, depth + 1)).append(
					printTrieByStats(node.G, depth + 1)).append(
					printTrieByStats(node.T, depth + 1)).append(
					printTrieByStats(node.$, depth + 1));
		}
		return builder.toString();
	}
}
