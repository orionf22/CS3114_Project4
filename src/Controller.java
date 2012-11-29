
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
	 * The {@link DNATrie} owned by this {@code Controller}.
	 */
	private DNATrie tree;
	/**
	 * The {@link Codec} used to translate information to and from bytes and a
	 * given format.
	 */
	private Codec<DNASequence> codec;

	/**
	 * Constructs a new {@code Controller} with references to proper
	 * {@link MemManager} and {@link DNATrie} objects.
	 * <p/>
	 * @param m the {@link MemManager} to use
	 * @param t the {@link DNATrie} to use
	 */
	public Controller(DNATrie t)
	{
		this.tree = t;
	}

	/**
	 * Sets the {@link Codec} to use during data translation.
	 * <p/>
	 * @param c the {@link Codec} to use
	 */
	public void setCodec(Codec<DNASequence> c)
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
		tree.insert(new DNASequence(c.getInfo()));
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
		tree.remove(sequence);
	}

	/**
	 * Prints the status of managed memory to {@link DNAFile#output}.
	 * <p/>
	 * @param c the {@link PrintCommand} to follow
	 */
	public void print(PrintCommand c)
	{
		DNAFile.output.println(tree.printTrie(c.getRequest()));
		//also print the free list and BufferPool status
		//DNAFile.output.println("\nFreeblock list:\n" + manager.getFreeBlocks()
		//		+ "\nBuffer Pool:\n" + bufferPool.getBlockIDs());
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
}
