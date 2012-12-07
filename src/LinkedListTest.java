import static org.junit.Assert.*;
import java.util.ListIterator;
import org.junit.Before;
import org.junit.Test;


public class LinkedListTest
{
    LinkedList<String> llist;
    ListIterator<String> iter;

    @Before
    public void setUp()
        throws Exception
    {
        llist = new LinkedList<String>();
        iter = llist.iterator();
    }


    @Test
    public void testToString()
    {/*
        assertEquals(llist.size(), 0);
        iter.add("Test");
        assertEquals(llist.size(), 1);
        System.out.println(llist.toString());
        assertEquals(true, "Test\n".equals(llist.toString()));
        iter.add("Test2");
        assertEquals(llist.size(), 2);
        iter.remove();
        assertEquals(llist.size(), 1);
        iter.add("Test3");
        iter.add("Test4");
        assertEquals(llist.size(), 3);
        System.out.println(llist.toString());
        assertEquals(true, "Test\nTest3\nTest4\n".equals(llist.toString()));
*/
    }


    @Test
    public void testSize()
    {
        assertEquals(llist.size(), 0);
        iter.add("Test");
        assertEquals(llist.size(), 1);
        iter.add("Test2");
        assertEquals(llist.size(), 2);
        iter.remove();
        assertEquals(llist.size(), 1);
        iter.add("Test3");
        iter.add("Test4");
        assertEquals(llist.size(), 3);
        iter.remove();
        iter.next();
        iter.remove();
        iter.next();
        iter.remove();
        assertEquals(llist.size(), 0);
    }


    @Test
    public void testIsEmpty()
    {
        assertEquals(llist.isEmpty(), true);
        iter.add("Test");
        assertEquals(llist.isEmpty(), false);
        iter.remove();
        assertEquals(llist.isEmpty(), true);
    }

    @Test
    public void testIter()
    {
        assertEquals(llist.isEmpty(), true);
        iter.add("Test1");
        assertEquals(llist.isEmpty(), false);
        assertEquals(iter.next().equals("Test1"), true);
        assertEquals(iter.next().equals("Test1"), true);
        assertEquals(iter.next().equals("Test1"), true);
        assertEquals(1, llist.size());
        iter.add("Test2");
        assertEquals(2, llist.size());
        assertEquals(iter.next().equals("Test2"), true);
        assertEquals(iter.next().equals("Test1"), true);
        assertEquals(iter.next().equals("Test2"), true);
        assertEquals(iter.next().equals("Test1"), true);
        iter.add("Test3");
        assertEquals(3, llist.size());
        assertEquals(iter.next().equals("Test3"), true);
        assertEquals(iter.next().equals("Test1"), true);
        assertEquals(iter.next().equals("Test2"), true);
        assertEquals(iter.next().equals("Test3"), true);
        iter.remove();
        assertEquals(2, llist.size());
        assertEquals(iter.next().equals("Test1"), true);
        assertEquals(iter.next().equals("Test2"), true);
        assertEquals(iter.next().equals("Test2"), true);
        iter.remove();
        assertEquals(1, llist.size());
        assertEquals(iter.next().equals("Test2"), true);
        assertEquals(iter.next().equals("Test2"), true);
        iter.remove();
        assertEquals(0, llist.size());
        assertEquals(false, llist.isEmpty());
        //assertEquals(iter.next().equals("Test2"), true);

    }

}
