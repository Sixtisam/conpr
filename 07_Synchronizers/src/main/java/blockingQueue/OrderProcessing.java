package blockingQueue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class OrderProcessing {

    public static void main(String[] args) {
        int nCustomers = 10;
        int nValidators = 8;
        int nProcessors = 4;
        BlockingQueue<Order> ordersToValidate = new LinkedBlockingQueue<>(1000);
        BlockingQueue<Order> ordersToExecute = new LinkedBlockingQueue<>(1000);

        for (int i = 0; i < nCustomers; i++) {
            new Customer("" + i, ordersToValidate).start();
        }

        for (int i = 0; i < nValidators; i++) {
            new OrderValidator(ordersToValidate, ordersToExecute).start();
        }

        for (int i = 0; i < nProcessors; i++) {
            new OrderProcessor(ordersToExecute).start();
        }

        while (true) {
            System.out.println(
                    " --------------------------------------------------- VALIDATE SIZE: " + ordersToValidate.size());
            System.out.println(
                    " --------------------------------------------------- EXECUTE SIZE: " + ordersToExecute.size());
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    static class Order {
        public final String customerName;
        public final int itemId;

        public Order(String customerName, int itemId) {
            this.customerName = customerName;
            this.itemId = itemId;
        }

        @Override
        public String toString() {
            return "Order: [name = " + customerName + " ], [item = " + itemId + " ]";
        }
    }

    static class Customer extends Thread {
        public final BlockingQueue<Order> ordersToValidate;

        public Customer(String name, BlockingQueue<Order> ordersToValidate) {
            super(name);
            this.ordersToValidate = ordersToValidate;
        }

        private Order createOrder() {
            Order o = new Order(getName(), (int) (Math.random() * 100));
            System.out.println("Created:   " + o);
            return o;
        }

        private void handOverToValidator(Order o) throws InterruptedException {
            ordersToValidate.put(o);
            // Variante mit warten:
//            if(!ordersToValidate.offer(o, 3, TimeUnit.SECONDS)) {
//                
//            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Order o = createOrder();
                    handOverToValidator(o);
                    Thread.sleep((long) (Math.random() * 1000));
                }
            } catch (InterruptedException e) {
            }
        }
    }

    static class OrderValidator extends Thread {
        public final BlockingQueue<Order> ordersToValidate;
        public final BlockingQueue<Order> ordersToExecute;

        public OrderValidator(BlockingQueue<Order> ordersToValidate, BlockingQueue<Order> ordersToExecute) {
            this.ordersToValidate = ordersToValidate;
            this.ordersToExecute = ordersToExecute;
        }

        public Order getNextOrder() throws InterruptedException {
            return ordersToValidate.take();
        }

        public boolean isValid(Order o) {
            return o.itemId < 50;
        }

        public void handOverToProcessor(Order o) throws InterruptedException {
            ordersToExecute.put(o);
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Order o = getNextOrder();
                    if (isValid(o)) {
                        handOverToProcessor(o);
                    } else {
                        System.err.println("Destroyed: " + o);
                    }
                    Thread.sleep((long) (Math.random() * 1000));
                }
            } catch (InterruptedException e) {
            }
        }
    }

    static class OrderProcessor extends Thread {
        public final BlockingQueue<Order> ordersToExecute;

        public OrderProcessor(BlockingQueue<Order> ordersToExecute) {
            this.ordersToExecute = ordersToExecute;
        }

        public Order getNextOrder() throws InterruptedException {
            return ordersToExecute.take();
        }

        public void processOrder(Order o) {
            System.out.println("Processed: " + o);
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Order o = getNextOrder();
                    processOrder(o);
                    Thread.sleep((long) (Math.random() * 1000));
                }
            } catch (InterruptedException e) {
            }
        }
    }
}
