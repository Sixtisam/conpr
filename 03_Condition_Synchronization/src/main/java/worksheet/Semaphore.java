package worksheet;

import java.util.LinkedList;
import java.util.Queue;

public final class Semaphore {
	private Object lock = new Object();
	private Queue<Thread> waitingThreads = new LinkedList<>();
	private int value;

	public Semaphore(int initial) {
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
