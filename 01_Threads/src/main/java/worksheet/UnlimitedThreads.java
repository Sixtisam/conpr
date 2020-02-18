package worksheet;

import java.io.IOException;

public class UnlimitedThreads {
	public static void main(String[] args) throws InterruptedException, IOException {
		System.out.println("Type to start");
		System.in.read();
		int i = 0;
		while (true) {
			i++;
			new Thread(() -> {
				while (true) {
					try {
						Thread.sleep(Integer.MAX_VALUE);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}).start();
			Thread.sleep(10);
			System.out.println("Created " + i);
		}
	}
}
