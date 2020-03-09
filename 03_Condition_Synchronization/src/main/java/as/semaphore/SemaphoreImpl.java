package as.semaphore;

public final class SemaphoreImpl implements Semaphore {
	private int value;

	public SemaphoreImpl(int initial) {
		if (initial < 0)
			throw new IllegalArgumentException();
		value = initial;
	}

	@Override
	public int available() {
		return value;
	}

	@Override
	public synchronized void acquire() {
		while (value == 0) {
			try {
				wait();
			} catch (InterruptedException e) {
				// nop
			}
		}
		value--;
	}

	@Override
	public synchronized void release() {
		value++;
		notifyAll();

	}
}
