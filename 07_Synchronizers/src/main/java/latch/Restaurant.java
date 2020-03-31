package latch;

import java.util.concurrent.CountDownLatch;

public class Restaurant {

    public static void main(String[] args) {
        int nrGuests = 2;
        int nrCooks = 2;

        CountDownLatch cookLatch = new CountDownLatch(nrCooks);
        CountDownLatch dishLatch = new CountDownLatch(nrGuests);

        for (int i = 0; i < nrCooks; i++) {
            new Cook(i, cookLatch).start();
        }

        for (int i = 0; i < nrGuests; i++) {
            new Guest(cookLatch, dishLatch).start();
        }

        new DishWasher(dishLatch).start();
    }

    static class Cook extends Thread {
        private final CountDownLatch latch;
        private final int nr;

        public Cook(int nr, CountDownLatch latch) {
            this.latch = latch;
            this.nr = nr;
        }

        public Cook(CountDownLatch latch) {
            this.latch = latch;
            this.nr = 0;
        }

        @Override
        public void run() {
            System.out.println("[" + nr + "] Start Cooking..");
            try {
                sleep(5000);
            } catch (InterruptedException e) {
            }
            System.out.println("[" + nr + "] Meal is ready");
            latch.countDown();
        }
    }

    static class Guest extends Thread {
        private final CountDownLatch cookLatch;
        private final CountDownLatch dishLatch;

        public Guest(CountDownLatch cookLatch, CountDownLatch dishLatch) {
            this.cookLatch = cookLatch;
            this.dishLatch = dishLatch;
        }

        @Override
        public void run() {
            try {
                sleep(1000);
                System.out.println("Entering restaurant and placing order.");
                cookLatch.await();
                System.out.println("Enjoying meal.");
                sleep(5000);
                System.out.println("Meal was excellent!");
                dishLatch.countDown();
            } catch (InterruptedException e) {
            }
        }
    }

    static class DishWasher extends Thread {
        private final CountDownLatch dishLatch;

        public DishWasher(CountDownLatch dishLatch) {
            this.dishLatch = dishLatch;
        }

        @Override
        public void run() {
            try {
                System.out.println("Waiting for dirty dishes.");
                dishLatch.await();
                System.out.println("Washing dishes.");
                sleep(0);
            } catch (InterruptedException e) {
            }
        }
    }
}
