
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

// On my honor:
//
// - I have not used source code obtained from another student,
// or any other unauthorized source, either modified or
// unmodified.
//
// - All source code and documentation used in my program is
// either my original work, or was derived by me from the
// source code published in the textbook for this course.
//
// - I have not discussed coding details about this project with
// anyone other than my partner (in the case of a joint
// submission), instructor, ACM/UPE tutors or the TAs assigned
// to this course. I understand that I may discuss the concepts
// of this program with other students, and that another student
// may help me debug my program so long as neither of us writes
// anything during the discussion or modifies any computer file
// during the discussion. I have violated neither the spirit nor
// letter of this restriction.
/**
 * Main class for Project 2, DNAFile. This driver class handles terminal
 * interfacing, command line argument parsing, and program setup tasks.
 * <p/>
 * This class should be invoked from the command line with only one parameter: a
 * path of a text file containing commands to execute. Command parsing is
 * handled in the {@link CommandParser} class; command execution is handled
 * here.
 * <p/>
 * @author orionf22
 */
public class DNAFile
{

	/**
	 * The output {@link PrintWriter}. All command output is directed here where
	 * it can be relayed to any acceptable {@link OutputStream} object.
	 */
	protected static PrintWriter output;
	/**
	 * The input {@link File} containing commands to parse and execute. This
	 * file should be a generic {@code .txt} file.
	 */
	protected static File inputFile;
	/**
	 * The {@link Controller} to use.
	 */
	protected static Controller controller;
	/**
	 * The {@link BufferedReader} that retrieves lines within {@code inputFile}
	 * for command parsing.
	 */
	private static BufferedReader input;
	
	/**
	 * The number of {@link Buffer} objects the {@link BufferPool} will manage.
	 */
	private static int buffers;
	/**
	 * The size in bytes each {@link Buffer} will manage.
	 */
	private static int blockSize;

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) throws IOException
	{
		inputFile = new File("src/commands.txt");
		//Parse arg[0]; this is expected to be the commands text file
		//if (!parseArgs(args))
		//{
			//Program cannot operate without a valid commands file
		//	System.exit(1);
		//}
		//Use System.out as the output
		output = new PrintWriter(System.out, true);
		input = new BufferedReader(new FileReader(inputFile));
		String line;
		controller = new Controller(new MemManager(100), new DNATrie());
		controller.setCodec(new DNACodec());

		while ((line = input.readLine()) != null)
		{
			//Get the command entered
			Command nextCommand = CommandParser.getNextCommand(line);
			//Checks to see what type of command was called, pointing control
			//to the appropriate function in the Controller

			if (nextCommand instanceof InsertCommand)
			{
				controller.insertRecord((InsertCommand) nextCommand);
			}
			else if (nextCommand instanceof RemoveCommand)
			{
				controller.removeRecord((RemoveCommand) nextCommand);
			}
			else if (nextCommand instanceof PrintCommand)
			{
				controller.print((PrintCommand) nextCommand);
			}
			else if (nextCommand instanceof SearchCommand)
			{
				controller.search((SearchCommand) nextCommand);
			}
		}
	}

	/**
	 * Parses command line arguments to ensure they conform to expected values.
	 * <p/>
	 * @param args the String array of command line arguments
	 * <p/>
	 * @return {@code true} if all three command line arguments are valid,
	 *            {@code false} otherwise
	 */
	private static boolean parseArgs(String[] args)
	{
		if (args == null || args.length < 2)
		{
			return false;
		}
		try
		{
			inputFile = new File(args[0]);
			inputFile.createNewFile();
		}
		catch (Exception e)
		{
			DNAFile.output.println("command-file \"" + args[0] + "\"not found\n");
			printInitializationFailure();
			return false;
		}
		try
		{
			buffers = Integer.parseInt(args[1]);
		}
		catch (Exception e)
		{
			DNAFile.output.println("Invalid buffer count of " + args[1] + "\n");
			return false;
		}
		try
		{
			blockSize = Integer.parseInt(args[2]);
		}
		catch (Exception e)
		{
			DNAFile.output.println("Invalid block size of " + args[2] + "\n");
			return false;
		}
		return true;
	}

	/**
	 * Print program initialization failure.
	 */
	public static void printInitializationFailure()
	{
		DNAFile.output.println("Memory Manager initialization failed.\n");
	}
}
