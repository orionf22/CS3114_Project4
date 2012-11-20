
import java.util.Scanner;

/**
 * {@code CommandParser} objects handle {@link Command} parsing. Input is fed as
 * a {@link String}, ideally one line at a time, and a {@link Scanner} used to
 * identify command keywords. Once a valid command is recognized further words
 * and values can be identified as appropriate, allowing commands with multiple
 * (or even no) parameters to be easily constructed.
 * <p/>
 * @author orionf22
 * @author rinaldi1
 */
public class CommandParser
{

	/**
	 * The current command in the process of parsing. If input is fed one line
	 * at a time, this value will also match up with the line number within the
	 * input source (such as a file). This can be helpful when debugging an
	 * unrecognized/invalid command.
	 */
	private static int commandNum;

	/**
	 * Parses {@code line} and determines what type of command to return.
	 * <p/>
	 * @param line the line to parse
	 * <p/>
	 * @return the {@link Command} type
	 */
	public static Command getNextCommand(String line)
	{
		if (line.equals(""))
		{
			return null;
		}
		Scanner sc = new Scanner(line);
		String commandType = sc.next();
		Command cmd = null;
		commandNum++;
		//If insert is the first word
		switch (commandType)
		{
			case "insert":
				String insertMe = "";
				//Check if DNA sequence exists
				try
				{
					insertMe = sc.next();
				}
				catch (Exception e)
				{
					DNAFile.output.println("INSERT, line " + commandNum
							+ ": no DNA sequence specified; expecting String. "
							+ "Command usage: insert <DNAString>.");
				}
				cmd = new InsertCommand(insertMe);
				break;
			//A remove command was encountered
			case "remove":
				String removeMe = "";
				//Check if DNA sequence exists
				try
				{
					removeMe = sc.next();
				}
				catch (Exception e)
				{
					DNAFile.output.println("REMOVE, line " + commandNum
							+ ": no DNA sequence specified; expecting String. "
							+ "Command usage: remove <DNAString>.");
				}
				cmd = new RemoveCommand(removeMe);
				break;
			//A print command was encountered
			case "print":
				String request = "";
				//check if print request exists
				try
				{
					request = sc.next();
				}
				catch (Exception e)
				{
					cmd = new PrintCommand(DNATrie.JUST_DO_IT_SON);
				}
				switch (request)
				{
					case "lengths":
						cmd = new PrintCommand(DNATrie.BY_LENGTH);
						break;
					case "stats":
						cmd = new PrintCommand(DNATrie.BY_STATS);
						break;
					case "":
						break;
					default:
						DNAFile.output.println("Print request \"" + request
								+ "\" not recognized. Call with no request, "
								+ "\"stats\", or \"lengths\".");
				}
				break;
				//A search command was encountered
			case "search":
				String param = "";
				//Check if DNA sequence exists
				try
				{
					param = sc.next();
				}
				catch (Exception e)
				{
					DNAFile.output.println("SEARCH, line " + commandNum + ": "
							+ "no DNA sequence specified; expecting String."
							+ "Command usage: search <sequenceDescriptor>.");
				}
				cmd = new SearchCommand(param);
				break;
			//No command recognized
			default:
				DNAFile.output.println("Command \"" + commandType
						+ "\" not recognized on line " + commandNum + "\n");
		}

		return cmd;
	}

	private CommandParser()
	{
		//no-op
	}
}
