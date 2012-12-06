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
	 *  Returns a ListIterator for this LinkedList
	 *
	 *  @author Anthony Rinaldi, Ryan Merkel
	 *  @version Dec 5, 2012
	 */
	protected class FreeListIterator
    implements ListIterator<E>
	{
	    boolean canremove = false;

        /**
         * New iterators start at the first Node in the list.
         */
        private Node<E> current = head.next;
        /**
         * Current iteration index within this list.
         */
        private int index = 0;

        @Override
        public void add(E item)
        {
            Node<E> newNode = new Node<>(item);
            // Empty List
            if (size == 0)
            {
                head.next = newNode;
                head.prev = newNode;
                newNode.prev = head;
                newNode.next = head;
            }
            // Not empty, but added to end
            else if (current.next == head)
            {
                current.next = newNode;
                newNode.prev = current;
                newNode.next = head;
                head.prev = newNode;
            }
            // Add in the middle
            else
            {
                Node<E> oldnext = current.next;
                current.next = newNode;
                oldnext.prev = newNode;
                newNode.prev = current;
                newNode.next = oldnext;
            }

            // Increment Size
            size++;
            canremove = true;
        }

        @Override
        public boolean hasNext()
        {
            return current.next != head;
        }

        @Override
        public boolean hasPrevious()
        {
            return current.prev != head;
        }

        @Override
        public E next()
        {
            E returning = current.value;
            if (current.next == head)
            {
                current = head.next;
            } else
            {
                current = current.next;
            }
            canremove = true;
            index++;
            return returning;
        }

        @Override
        public int nextIndex()
        {
            return index + 1;
        }

        @Override
        public E previous()
        {
            E returning = current.value;
            if (current.prev == head)
            {
                current = head.prev;
                index = size - 1;
            } else
            {
                current = current.prev;
                index--;
            }


            return returning;
        }

        @Override
        public int previousIndex()
        {
            return index - 1;
        }

        @Override
        public void remove()
        {
            assert (canremove == true);
            {
                if (size == 1)
                {
                    head.next = head;
                    head.prev = head;
                    current = head;

                }
                else
                {
                    current.prev.next = current.next;
                    current.next.prev = current.prev;
                    current = current.prev;

                }
                size--;
                canremove = false;
            }


        }

        @Override
        public void set(E val)
        {
            current.value = val;

        }

	}



    // ----------------------------------------------------------
    /**
     * Gives the size
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
