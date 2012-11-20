
import java.util.ListIterator;

/**
 * Simple doubly-linked and circular list. Makes use of a Node subclass.
 * <p/>
 * @author orionf22
 * @author rinaldi1
 */
public class LinkedList<E>
		implements Iterable<E>
{

	/**
	 * The size of this list.
	 */
	private int size;
	/**
	 * The head of this list.
	 */
	private Node<E> head;
	/**
	 * The tail of this list.
	 */
	private Node<E> tail;

	/**
	 * Creates a new {@code LinkedList} initialized to size {@code s}. The list
	 * is initially completely free, so only one {@code MemBlock} object fills
	 * the entire list. {@code sentinel} is linked to itself as the free list
	 * operates using a doubly-linked list.
	 */
	public LinkedList()
	{
		head = new Node<>(null);
		tail = new Node<>(null);
		tail.prev = head;
		tail.next = head;
		head.next = tail;
		head.prev = tail;
		size = 0;
	}

	@Override
	public String toString()
	{
		ListIterator<E> iter = iterator();
		String result = "";
		int i = 0;
		while (i < 6)
		{
			result += iter.next() + "\n";
			i++;
		}

		return result;
	}

	/**
	 * Returns the size of this list.
	 * <p/>
	 * @return the size of this list
	 */
	public int size()
	{
		return this.size;
	}

	/**
	 * Determines if this list is empty.
	 * <p/>
	 * @return {@code true} if the size is zero, {@code false} otherwise
	 */
	public boolean isEmpty()
	{
		return size == 0;
	}

	@Override
	public FreeListIterator iterator()
	{
		return new FreeListIterator();
	}

	protected class FreeListIterator
			implements ListIterator<E>
	{

		/**
		 * New iterators start at the first Node in the list.
		 */
		private Node<E> current = head.next;
		/**
		 * Current iteration index within this list.
		 */
		private int index = 0;

		@Override
		public boolean hasNext()
		{
			return index < size;
		}

		@Override
		public boolean hasPrevious()
		{
			return index > 0;
		}

		@Override
		public int previousIndex()
		{
			return index - 1;
		}

		@Override
		public int nextIndex()
		{
			return index;
		}

		@Override
		public E next()
		{
			if (current.next != tail)
			{
				current = current.next;
			}
			else
			{
				current = head.next;
			}
			index++;
			return current.value;
		}

		@Override
		public E previous()
		{
			current = current.prev;
			index--;
			return current.value;
		}

		@Override
		public void set(E item)
		{
			current.value = item;
		}

		@Override
		public void remove()
		{
			Node<E> previous = current.prev;
			Node<E> next = current.next;
			previous.next = next;
			next.prev = previous;
			size--;
			index--;
			if (size == 0)
			{
				current = head;
			}
			else if (current.next == tail)
			{
				current = current.prev;
			}
		}

		@Override
		public void add(E item)
		{
			Node<E> newNode = new Node<>(item);
			//System.out.println(head + ": " + head.next + ": " + current + ": " + tail + ": " + tail.next + ": " + head.prev);
			Node<E> next = current.next;
			if (size == 0)
			{
				next = tail;
				current = head;
			}
			else if (next == tail)
			{
				next = head.next;
			}
			next.prev = newNode;
			newNode.prev = current;
			current.next = newNode;
			newNode.next = next;
			size++;
			index++;
			current = newNode;
			//System.out.println(head + ": " + head.next + ": " + current + ": " + tail + ": " + tail.next + ": " + head.prev);
		}
		
		public E current()
		{
			return this.current.value;
		}
	}

	/**
	 * Helper class that represents a Node within a doubly-linked list.
	 * <p/>
	 * @param <Q> the generic datatype stored by this Node
	 */
	private class Node<Q>
	{

		private Node<Q> next;
		private Node<Q> prev;
		private Q value;

		private Node(Q val)
		{
			this.value = val;
		}
	}
}
