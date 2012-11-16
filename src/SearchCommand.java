
/**
 * {@code SearchCommand} objects wrap a single String sequence for the purposes
 * of later accessing that sequence to find it within a database.
 * <p/>
 * @author orionf22
 * @author rinaldi1
 */
public class SearchCommand
		implements Command
{

	/**
	 * The sequence for which to search.
	 */
	private String sequence;

	/**
	 * Constructs a new {@code SearchCommand} from {@code seq}.
	 * 
	 * @param seq the sequence to store
	 */
	public SearchCommand(String seq)
	{
		this.sequence = seq;
	}

	/**
	 * Returns the sequence for which to search.
	 * 
	 * @return {@code sequence}
	 */
	public String getSequence()
	{
		return this.sequence;
	}
}
