package spotbugs;

import java.util.LinkedList;

public class I extends LinkedList<Integer> {
    public I() {
        new Thread(new Runnable() {
            public void run() {
                add(1);
            }
        }).start();
    }
}