package as.list;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;

public class CopyOnWriteList<E> implements CoWList<E> {

    private final AtomicReference<LinkedList<E>> list = new AtomicReference<>(new LinkedList<>());

    @Override
    public Iterator<E> iterator() {
        final Iterator<E> listIterator = list.get().iterator();
        return new Iterator<E>() {
            private E lastNext;
            @Override
            public boolean hasNext() {
                return listIterator.hasNext();
            }

            @Override
            public E next() {
                return lastNext = listIterator.next();
            }
            
            /**
             * Variant 2: Removes the last element returned by next
             */
            @Override
            public void remove() {
                while (true) {
                    LinkedList<E> current = list.get();
                    @SuppressWarnings("unchecked")
                    LinkedList<E> newList = (LinkedList<E>) current.clone();
                    newList.remove(lastNext);
                    if (list.compareAndSet(current, newList)) {
                        return;
                    }
                }
            }
        };
    }

    @Override
    public void addFirst(E e) {
        while (true) {
            LinkedList<E> current = list.get();
            @SuppressWarnings("unchecked")
            LinkedList<E> newList = (LinkedList<E>) current.clone();
            newList.addFirst(e);
            if (list.compareAndSet(current, newList)) {
                return;
            }
        }

    }

    @Override
    public void removeFirst() {
        while (true) {
            LinkedList<E> current = list.get();
            @SuppressWarnings("unchecked")
            LinkedList<E> newList = (LinkedList<E>) current.clone();
            newList.removeFirst();
            if (list.compareAndSet(current, newList)) {
                return;
            }
        }
    }

    @Override
    public int size() {
        return list.get().size();
    }

}
