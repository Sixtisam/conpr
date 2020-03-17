package as.semaphore;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Faire semaphore
 *
 */
public final class SemaphoreImpl implements Semaphore {
    private Object lock = new Object();
    private Queue<Thread> waitingThreads = new LinkedList<>();
    private int value;

    public SemaphoreImpl(int initial) {
        if (initial < 0)
            throw new IllegalArgumentException();
        value = initial;
    }

    public int available() {
        synchronized (lock) {
            return value;
        }
    }

    public void acquire() {
        synchronized (lock) {
            if (value <= 0) {
                waitingThreads.add(Thread.currentThread());
                while (value <= 0 || waitingThreads.peek() != Thread.currentThread()) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        // nop
                    }
                }
                waitingThreads.poll();
                
                // This notifyAll is required!
                // When 2 threads (TW) are waiting and then, 2 other threads  release the semaphore
                // TW2 gets the lock, he recognizes he is not the first thread in the queue and waits again
                // TW1 gets the lock, he recognize he is the first and acquires the lock.
                // --> TW2 is now still waiting even though the semaphore has 1 free place.
                lock.notifyAll();
                value--;
            } else {
                value--;
            }
        }
    }

    public void release() {
        synchronized (lock) {
            value++;
            lock.notifyAll();
        }
    }
}
