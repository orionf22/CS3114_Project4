
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
	private Node<E> current;

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
		current = head;
		tail.prev = head;
		head.next = tail;
		size = 0;
	}

	public void append(E item)
	{
		Node<E> newNode = new Node<>(item);
		Node<E> previous = tail.prev;
		tail.prev = newNode;
		newNode.next = tail;
		previous.next = newNode;
		newNode.prev = previous;
		size++;
	}

	public void insert(E item)
	{
		Node<E> newNode = new Node<>(item);
		Node<E> next = current.next;
		current.next = newNode;
		newNode.prev = current;
		newNode.next = next;
		next.prev = newNode;
		size++;
	}

	/**
	 * Removes the next element relative to {@code current}. For example, if the
	 * current list is 1<-->2<-->3<-->4 and {@code current} is currently at
	 * {@code 2}, the node containing {@code 3} will be removed and 2 and 4 will
	 * be linked together.
	 * <p/>
	 * @return
	 */
	public E remove()
	{
		if (current.next == tail)
		{
			return null;
		}
		E ret = current.next.value;
		Node<E> next = current.next.next;
		current.next = next;
		next.prev = current;
		size--;
		return ret;
	}

	@Override
	public String toString()
	{
		ListIterator<E> iter = iterator();
		String result = "";
		while (iter.hasNext())
		{
			result += iter.next().toString() + "\n";
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
	public ListIterator<E> iterator()
	{
		return new FreeBlockIterator();
	}

	private class FreeBlockIterator
			implements ListIterator<E>
	{

		/**
		 * New iterators start at the first Node in the list.
		 */
		private Node<E> current = head.next;
		/**
		 * Last accessed node.
		 */
		private Node<E> lastAccessed = null;
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
			lastAccessed = current;
			E item = current.value;
			if (current.next != tail)
			{
				current = current.next;
			}
			else
			{
				current = head.next;
			}
			index++;
			return item;
		}

		@Override
		public E previous()
		{
			current = current.prev;
			index--;
			lastAccessed = current;
			return current.value;
		}

		@Override
		public void set(E item)
		{
			lastAccessed.value = item;
		}

		@Override
		public void remove()
		{
			Node<E> previous = lastAccessed.prev;
			Node<E> next = lastAccessed.next;
			previous.next = next;
			next.prev = previous;
			size--;
			index--;
			if (current.next != tail)
			{
				current = current.next;
			}
			else
			{
				current = head.next;
			}
			lastAccessed = null;
		}

		@Override
		public void add(E item)
		{

			Node<E> next = current.next;
			if (next == tail)
			{
				next = head.next;
			}
			Node<E> newNode = new Node<>(item);
			Node<E> curr = current;
			next.prev = newNode;
			newNode.prev = curr;
			curr.next = newNode;
			newNode.next = next;
			size++;
			index++;
			lastAccessed = null;
			if (size == 1)
			{
				current = newNode;
			}
			/*
			 Node<E> newNode = new Node<>(item);
			 Node<E> oldFirst = sentinel.next;
			 sentinel.next = newNode;
			 newNode.next = oldFirst;
			 oldFirst.prev = newNode;
			 newNode.prev = sentinel;
			 size++;
			 index++;
			 lastAccessed = null;
			 //current = newNode;
			 */
		}
	}

	/**
	 * Helper class that represents a Node within a doubly-linked list.
	 * <p/>
	 * @param <E> the generic datatype stored by this Node
	 */
	private class Node<E>
	{

		private Node<E> next;
		private Node<E> prev;
		private E value;

		private Node(E val)
		{
			this.value = val;
		}
	}
}
