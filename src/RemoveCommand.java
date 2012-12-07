/**
 * This class wraps String sequence for the purposes of removing that sequence.
 * <p/>
 * @author orionf22
 * @author rinaldi1
 */
public class RemoveCommand
		implements Command
{

	/**
	 * The sequence to remove.
	 */
	private String sequence;

	/**
	 * Constructs a new {@code RemoveCommand} with a sequence of {@code seq}.
	 * <p/>
	 * @param seq the removal sequence
	 */
	public RemoveCommand(String seq)
	{
		this.sequence = seq;
	}

	/**
	 * Returns the removal sequence of this command.
	 * <p/>
	 * @return the removal sequence
	 */
	public String getSequence()
	{
		return this.sequence;
	}
}
