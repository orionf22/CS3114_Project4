
/**
 * This class wraps a String info for the purposes of inserting {@code info}
 * @author orionf22
 * @author rinaldi1
 */
public class InsertCommand
		implements Command
{

	private String info;

	/**
	 * Constructs a new {@code InsertCommand} from {@code i} and {@code stuff}.
	 * <p/>
	 * @param stuff the String to insert
	 */
	public InsertCommand(String stuff)
	{
		this.info = stuff;
	}

	/**
	 * Returns the String info of this {@code InsertCommand}.
	 * <p/>
	 * @return the info of this command
	 */
	public String getInfo()
	{
		return this.info;
	}
}
