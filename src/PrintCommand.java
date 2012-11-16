
/**
 * This class wraps an int value for the purposes of printing information from a
 * {@link DNATrie}. It is not the duty of the {@code PrintCommand} class to
 * determine what request codes are valid or invalid, or what they mean. That
 * duty belongs to whatever class is actually printing. {@code PrintCommand}
 * commands simply maintain whatever code was given to them.
 * <p/>
 * @author orionf22
 * @author rinaldi1
 */
public class PrintCommand
		implements Command
{

	/**
	 * The request code.
	 */
	private int request;

	/**
	 * Constructs a new {@code PrintCommand} with a request code of {@code 0}.
	 */
	public PrintCommand()
	{
		this.request = 0;
	}

	/**
	 * Constructs a new {@code PrintCommand} with a request code of {@code req}.
	 * <p/>
	 * @param req the request code
	 */
	public PrintCommand(int req)
	{
		this.request = req;
	}

	/**
	 * Retrieves the request code of this command.
	 * <p/>
	 * @return the request code
	 */
	public int getRequest()
	{
		return this.request;
	}
}
