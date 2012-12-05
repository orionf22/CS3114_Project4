
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;

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
 * This project stores DNA sequences (defined in the {@link DNASequence} class)
 * in a {@link DNATrie} for easy sorting and finding operations. Data is
 * introduced in the form of {@link Command commands}
 * <p/>
 * Up to a certain point, the more {@link Buffer Buffers} managed by the
 * {@link BufferPool} the faster the program will execute (due to minimized disk
 * accesses). This number is the second argument on the command line.
 * <p/>
 * Finally, program execution statistics are collected and stored in a new file
 * specified by the third and final command line argument. This argument is
 * expected to be a valid abstract pathname which will be used to create a new
 * file.
 * <p/>
 * @author orionf22
 * @author rinaldi1
 */
public class DNAFile
{

	/**
	 * The number of {@link Buffer Buffers} that the {@link BufferPool} is
	 * allowed to manage.
	 */
	private static int buffers;
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
	 * A publicly available {@link PrintWriter}. All program output comes
	 * through this writer.
	 */
	public static PrintWriter output;
	/**
	 * The input {@link File} containing all commands to follow during
	 * execution.
	 */
	public static File inputFile;
	/**
	 * The size, in bytes, each {@link Buffer} will manage.
	 */
	public static int BLOCK_SIZE;
	/**
	 * The {@code .dat} file containing all data necessary for execution. This
	 * includes, but is not limited to, {@link TrieNode} contents and
	 * {@link DNATrie} data. This file serves as the {@link MemoryPool} used by
	 * the {@link MemManager}.
	 */
	public static File BIN_DAT = new File("p4bin.dat");

	/**
	 * @param args the command line arguments
	 * <p/>
	 * @throws HeapException
	 */
	public static void main(String[] args) throws IOException
	{
		output = new PrintWriter(System.out, true);
		MemManager manager = new MemManager(1024, BLOCK_SIZE, buffers, BIN_DAT);
		controller = new Controller(new DNATrie(manager));
		//parse the command line arguments. the program cannot operate if any 
		//arguments are invalid
		if (!parseArgs(args))
		{
			output.println("Program initialization failed.");
			System.exit(1);
		}
		String line;
		input = new BufferedReader(new FileReader(inputFile));
		while ((line = input.readLine()) != null)
		{
			//Get the command entered
			Command nextCommand = CommandParser.getNextCommand(line);
			//Checks to see what type of command was called, pointing
			//control to the appropriate function in the Controller

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
		//close the controller, which will close its underlying data structures
		controller.close();
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
			output.println("Invoke as DNAFile <command-file> <num-buffers> "
					+ "<block-size>");
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
			BLOCK_SIZE = Integer.parseInt(args[2]);
		}
		catch (Exception e)
		{
			DNAFile.output.println("Invalid block size of " + args[2] + "\n");
			return false;
		}
		return true;
	}
}
