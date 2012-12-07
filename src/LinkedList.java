
import java.util.ListIterator;

/**
 * Simple doubly-linked and circular list. Makes use of a Node subclass.
 * <p/>
 * @author orionf22
 * @author rinaldi1
 * @param <E> Generic
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

	// ----------------------------------------------------------
	/**
	 * LinkedList constructor: Creates empty linked list.
	 */
	LinkedList()
	{
		head = new Node<>(null);
		head.next = head;
		head.prev = head;
		size = 0;
	}

	@Override
	public String toString()
	{
		return null;
		// TODO Work it!
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

	@Override
	public FreeListIterator iterator()
	{
		return new FreeListIterator();
	}

	// -------------------------------------------------------------------------
	/**
	 * Returns a ListIterator for this LinkedList
	 * <p/>
	 * @author Anthony Rinaldi, Ryan Merkel
	 * @version Dec 5, 2012
	 */
	protected class FreeListIterator
			implements ListIterator<E>
	{

		boolean canremove = false;
		/**
		 * New iterators start at the first Node in the list.
		 */
		private Node<E> next;
		private Node<E> lastReturned = null;
		/**
		 * Current iteration index within this list.
		 */
		private int nextIndex = 0;
		
		FreeListIterator()
		{
			next = head.next;
		}

		@Override
		public void add(E item)
		{
			Node<E> newNode = new Node<>(item);
			// Empty List
			lastReturned = null;
			if (next == null)
			{
				head.next = newNode;
				head.prev = newNode;
				newNode.prev = head;
				newNode.next = head;
			}
			else
			{
				Node<E> oldPrev = next.prev;
				
				oldPrev.next = newNode;
				newNode.next = next;
				next.prev = newNode;
				newNode.prev = oldPrev;
			}
			// Increment Size
			size++;
			nextIndex++;
		}

		@Override
		public boolean hasNext()
		{
			return nextIndex < size;
		}

		@Override
		public boolean hasPrevious()
		{
			return nextIndex > 0;
		}

		@Override
		public E next()
		{
			lastReturned = next;
			if (!hasNext())
			{
				next = head.next;
			}
			else
			{
				next = next.next;
			}
			nextIndex++;
			return lastReturned.value;
		}

		@Override
		public int nextIndex()
		{
			return nextIndex;
		}

		@Override
		public E previous()
		{
			E returning = next.value;
			if (next.prev == head)
			{
				next = head.prev;
				nextIndex = size - 1;
			}
			else
			{
				next = next.prev;
				nextIndex--;
			}


			return returning;
		}

		@Override
		public int previousIndex()
		{
			return nextIndex - 1;
		}

		@Override
		public void remove()
		{
			assert (lastReturned != null);
			Node<E> lastNext = lastReturned.next;
			Node<E> prev = lastReturned.prev;
			prev.next = lastNext;
			lastNext.prev = prev;
			if (next == lastReturned)
			{
				next = lastNext;
			}
			else
			{
				nextIndex--;
			}
			size--;
			lastReturned = null;
		}

		@Override
		public void set(E val)
		{
			next.value = val;
		}
	}

	// ----------------------------------------------------------
	/**
	 * Gives the size
	 * <p/>
	 * @return size
	 */
	public int size()
	{
		return size;
	}

	public boolean isEmpty()
	{
		return size == 0;
	}
}
