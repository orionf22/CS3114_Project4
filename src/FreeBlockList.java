
import java.util.ListIterator;

/**
 * The {@code FreeBlockList} manages free memory space by use of a circular,
 * doubly-linked list. Data is not managed here, but requests for new storage
 * must come through here after first passing through the {@link RecordArray}.
 * <p/>
 * In the step-by-step process of handling memory record requests: <ul><li>The
 * {@link Controller} queries its {@link RecordArray} to see if it can store a
 * new record.</li> <li>If space is available, the {@link MemManager} will
 * request enough free space from its {@code FreeBlockList}.</li><li>If there is
 * enough free space, it is removed from the {@code FreeBlockList} and a new
 * block of memory is passed to the {@link MemoryPool}.</li><li>The memory pool
 * uses the newly allocated space to store the request and the
 * {@link MemManager} passes up a {@link MemHandle} that can be used by
 * higher-level classes to retrieve information stored in the
 * {@link MemoryPool}.</li> </ul>
 * <p/>
 * The {@code iter} object is used to follow the <b>Circular First Fit</b> rule.
 * Because this {@link LinkedList.FreeBlockIterator} is constantly used and
 * never reinitialized/reset, its position is always that of the last visited
 * free block. Thus, the next block is examined for memory requests rather than
 * starting at the front of the list and traversing through it.
 * <p/>
 * @author orionf22
 * @author rinaldi1
 */
public class FreeBlockList
{

	/**
	 * The {@link LinkedList} wrapped by this class.
	 */
	private LinkedList<MemBlock> freeList;
	/**
	 * The member {@link LinkedList.FreeBlockIterator} maintained by this
	 * {@code FreeBlockList}.
	 */
	private ListIterator<MemBlock> iter;

	/**
	 * Constructs a new {@code FreeBlockList} of the given size {@code s}.
	 * <p/>
	 * @param s the size of the list
	 */
	public FreeBlockList(int s)
	{
		freeList = new LinkedList<>();
		iter = freeList.iterator();
		//freeList.insert(new MemBlock(0, s));
		iter.add(new MemBlock(0, s));
	}

	/**
	 * Retrieves the number of free blocks this {@code FreeBlockList} can store.
	 * <p/>
	 * @return the size of this {@code FreeBlockList}
	 */
	public int capacity()
	{
		return freeList.size();
	}
	
	public boolean verify()
	{
		ListIterator<MemBlock> ver = freeList.iterator();
		int[] vals = new int[freeList.size()];
		int j = 0;
		while (j < freeList.size())
		{
			MemBlock got = ver.next();
			vals[j] = got.getAddress();
			j++;
		}
		for (int i = 0; i < vals.length - 1; i++)
		{
			if (vals[i] == vals[++i])
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Request a new memory block allocation of size {@code s} from this
	 * {@code FreeBlockList}. If the list is empty, a {@link MemHandle} is
	 * returned with an address of {@code -1}, similar to returning null. This
	 * negative value means no space is available for allocating new memory
	 * requests.
	 * <p/>
	 * Otherwise, if there is some free space, {@code iter} is used to traverse
	 * {@code freeList} to find the first {@link MemBlock} big enough to satisfy
	 * the request. The <b>Circular First Fit</b> rule is at work here; because
	 * the iteration is done using {@code iter} and not a new
	 * {@link LinkedList.FreeBlockIterator}, the position in the free list can
	 * be maintained. When a suitable {@link MemBlock} is located, it is removed
	 * from {@code freeList} and a new {@link MemBlock} is created to fill the
	 * remainder of the space the old block occupied, thus allocating only
	 * enough bytes to satisfy the request (extra bytes are preserved in the
	 * free list}. A {@link MemHandle} addressing the start of this allocated
	 * memory is returned.
	 * <p/>
	 * If there is no {@link MemBlock} large enough to satisfy the request, a
	 * {@link MemHandle} of address {@code -1} is returned, indicating that
	 * there is no space for the request in memory.
	 * <p/>
	 * This method is the inverse of {@link reclaimSpace(int)}.
	 * <p/>
	 * @param s the size in bytes of the request
	 * <p/>
	 * @return a {@link MemHandle} addressing the start of the allocated memory
	 *            block
	 */
	public MemHandle getSpace(int s)
	{
		verify();
		if (!freeList.isEmpty())
		{
			MemBlock first = iter.next();
			MemBlock current = first;
			do
			{
				if (current.getSize() >= s)
				{
					iter.remove();
					int ret = current.getAddress();
					MemBlock remainder = new MemBlock(ret + s,
							current.getSize() - s);
					iter.add(remainder);
					verify();
					return new MemHandle(ret);
				}
			}
			while ((current = iter.next()) != first);
		}
		return new MemHandle(-1);
	}

	/**
	 * Reclaims free space from memory denoted by the address of {@code h} of
	 * size {@code s}. When memory is removed or otherwise marked for garbage
	 * collection, its space is reclaimed in the free list. {@code h} marks the
	 * start in memory of this reclamation; {@code s} denotes how much memory is
	 * to be freed.
	 * <p/>
	 * To free space, a new {@link MemBlock} must be created and inserted into
	 * the free list. However, existing {@link MemBlock} objects may be
	 * immediately adjacent to the new free space. If this is the case, these
	 * blocks must be merged into one, larger block. This method handles all
	 * four cases of memory adjacency:<ul><li>there are no adjacent free
	 * blocks</li><li>there is a free block immediately preceding or proceeding
	 * the new free block</li><li>there are free blocks immediately preceding
	 * and proceeding the new free block</li></ul>
	 * <p/>
	 * In each case, appropriate action is taken, merging affected free blocks
	 * if necessary, but ultimately adding the new free block into the list.
	 * <p/>
	 * @param h the {@link MemHandle} marking the address of the free space
	 * @param s the size of the new free space
	 */
	public void reclaimSpace(MemHandle h, int s)
	{
		verify();
		ListIterator<MemBlock> newIter = freeList.iterator();
		//left and right denote free blocks adjacent to the new free block being
		//added
		MemBlock left = null;
		MemBlock right = null;
		while (newIter.hasNext() && (left == null || right == null))
		{
			MemBlock block = newIter.next();
			int leftSum = block.getAddress() + block.getSize();
			int rightSum = h.getAddress() + s;
			//left-adjacent if the address of h equals the address of the
			//current block plus its size plus one
			if (leftSum == h.getAddress())
			{
				newIter.remove();
				left = block;
			}
			//right-adjacent if the address of the current block equals the
			//address of h plus the s (both params)
			else if (rightSum == block.getAddress())
			{
				newIter.remove();
				right = block;
			}
		}
		MemBlock newBlock;
		int start;
		int size = s;
		//if left is not null, there is a left-adjacent block. the new block
		//must address here, and the size will be the size of the new block
		//plus the size of the left block
		if (left != null)
		{
			start = left.getAddress();
			size += left.getSize();
		}
		//if left is null, the address of the new block is simply the address of
		//h (param)
		else
		{
			start = h.getAddress();
		}
		//if right is not null, the address will be unaffected from the previous
		//two conditionals, but the size must change to include the size
		//determined above plus the size of the right block. in cases where
		//there is a left-adjacent block, the final size will be the sum of the
		//sizes of the left, new and right blocks. in cases where the right is
		//the only adjacent block, the size will be the sum of the new block
		//plus that of the right
		if (right != null)
		{
			size += right.getSize();
		}
		//construct a new MemBlock then insert
		newBlock = new MemBlock(start, size);
		newIter.add(newBlock);
		verify();
	}

	/**
	 * Returns a String representation of this {@code FreeBlockList}. The block
	 * currently to which {@code iter} is currently pointing is marked by an
	 * asterisk (*). All free blocks are printed in order of traversal,
	 * regardless of whether or not it is the current free block or not.
	 * <p/>
	 * Note that this method could override {@code toString()} but instead is
	 * distinct because it only returns a representation of what is in the list,
	 * not other key information about the {@code FreeBlockList} itself (such as
	 * the status of {@code iter}, the size of the list, how many blocks
	 * currently occupy the list, etc.).
	 * <p/>
	 * @return the String representation of this {@code FreeBlockList}
	 */
	public String blocksToString()
	{
		ListIterator<MemBlock> blocksIter = freeList.iterator();
		String val = "";
		MemBlock current = iter.next();
		while (blocksIter.hasNext())
		{
			MemBlock next = blocksIter.next();
			//if the current block is equal to the block that the iter member is
			//at, then this is the currently examined free block and needs to be
			//marked by an asterisk
			if (next.equals(current))
			{
				val += "*";
			}
			//add the String representation of this MemBlock
			val += next.toString();
			//if there's a next block, add a comma delimiter
			if (blocksIter.hasNext())
			{
				val += ", ";
			}
		}
		return val;
	}
	
	private class Node<Q>
	{
		Q value;
		Node<Q> next;
		Node<Q> prev;
		
		private Node(Q val)
		{
			this.value = val;
		}
	}
}
