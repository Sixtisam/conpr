package atomics;

import java.util.concurrent.atomic.AtomicReference;

public class NumberRangeWrong {

    // INVARIANT: lower <= upper is NOT GUARANTEED
    public static class Pair {
        public final int lower;
        public final int upper;

        public Pair(int lower, int upper) {
            this.lower = lower;
            this.upper = upper;
        }

    }

    private final AtomicReference<Pair> mem = new AtomicReference<>(new Pair(0, 0));

    public int getLower() {
        return mem.get().lower;
    }

    public void setLower(int i) {
        while (true) {
            Pair knownMem = mem.get();
            int u = knownMem.upper;
            if (i > u)
                throw new IllegalArgumentException();
            Pair newMem = new Pair(i, u);
            if (mem.compareAndSet(knownMem, newMem))
                return;
        }
    }

    public int getUpper() {
        return mem.get().upper;
    }

    public void setUpper(int i) {
        while (true) {
            Pair knownMem = mem.get();
            int l = knownMem.lower;
            if (i < l)
                throw new IllegalArgumentException();
            Pair newMem = new Pair(l, i);
            if (mem.compareAndSet(knownMem, newMem))
                return;
        }
    }

    public boolean contains(int i) {
        Pair knownMem = mem.get();
        return knownMem.lower <= i && i <= knownMem.upper;
    }
}
