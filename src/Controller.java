
import java.io.IOException;
import java.util.ArrayList;

/**
 * The {@code Controller} class manages records via a {@link DNATrie} to
 * maintain a proper database of stored and available records (data). Various
 * commands are sent here for execution.
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
	 * Closes the {@link DNATrie} {@link MemManager}. THis ensures any in-memory
	 * data is written to disk.
	 * <p/>
	 * @throws IOException
	 */
	public void close() throws IOException
	{
		tree.getManager().close();
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
