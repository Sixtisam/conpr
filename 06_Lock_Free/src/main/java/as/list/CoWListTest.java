package as.list;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.Test;

public class CoWListTest {

    public CoWList<Integer> createList() {
        return new CopyOnWriteList<>();
    }

    @Test(expected = NoSuchElementException.class)
    public void testEmptyList() {
        CoWList<Integer> l = createList();
        assertEquals(0, l.size());
        l.removeFirst();
    }

    @Test
    public void testInsert() {
        CoWList<Integer> l = createList();
        assertEquals(0, l.size());
        l.addFirst(1);
        assertEquals(1, l.size());
        l.addFirst(42);
        assertEquals(2, l.size());
    }

    @Test
    public void testIterator() {
        CoWList<Integer> l = createList();
        l.addFirst(1);
        l.addFirst(42);

        Iterator<Integer> it = l.iterator();
        int i = 0;
        while (it.hasNext()) {
            int next = it.next();
            if (i == 0) assertEquals(42, next);
            else if (i == 1) assertEquals(1, next);
            else if (i == 2) fail("Too many elements!");

            i++;
        }
        assertEquals(2, i);
    }

    @Test
    public void testIteratorNoModException() {
        CoWList<Integer> l = createList();
        l.addFirst(1);
        l.addFirst(42);

        for (@SuppressWarnings("unused") Integer integer : l) {
            l.addFirst(3); // Modification during iteration
            l.addFirst(8);
        }
    }
    
    @Test
    public void testIteratorRemove() {
        CoWList<Integer> l = createList();
        l.addFirst(1);
        l.addFirst(42);
        l.addFirst(99);

        Iterator<Integer> iter = l.iterator();
        while(iter.hasNext()) {
            iter.next();
            iter.remove();
        }
        
        assertEquals(l.size(), 0);
    }
    
    @Test
    public void testThreads() {
        CoWList<Integer> list = createList();
        
        Thread t1 = new Thread(() -> {
            for(int i = 0; i < 1000; i+=2) {
                list.addFirst(i);
                System.out.println("t1");
            }
        });
        t1.start();
        Thread t2 = new Thread(() -> {
            for(int i = 1; i < 1000; i+=2) {
                list.addFirst(i);
                System.out.println("t2");
            }
        });
        t2.start();
        
        try {
            t1.join();
            t2.join();
            
            for(Integer i : list) {
                System.out.print(i + " ");
            }
            assertEquals(1000, list.size());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
