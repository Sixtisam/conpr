package as.conbench;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BenchmarkRunnerImpl implements BenchmarkRunner {
    public void runBenchmark(BenchmarkDescriptor benchDesc) {
        try {
            Object testObject = benchDesc.testClass.getConstructor().newInstance();

            int nrCases = benchDesc.testMethods.get(0).nThreads.length;
            int nTimes = benchDesc.nTimes;

            // Warmup phase
            System.out.println("Warming up");
            for (BenchmarkMethodDescriptor mDesc : benchDesc.testMethods) {
                mDesc.method.invoke(testObject, benchDesc.nTimes, 1);
            }

            System.out.println("Starting benchmark..");
            for (int i = 0; i < nrCases; i++) {
                // get number of total threads running concurrently
                int participants = 0;
                LinkedList<String> methodStr = new LinkedList<>();
                for (BenchmarkMethodDescriptor methodDesc : benchDesc.testMethods) {
                    participants += methodDesc.nThreads[i];
                    methodStr.add(methodDesc.method.getName() + "(" + methodDesc.nThreads[i] + ")");
                }

                // barriers which will 'start' the benchmark
                CyclicBarrier barrier = new CyclicBarrier(participants);
                ExecutorService svc = Executors.newFixedThreadPool(participants);
                List<Callable<Long>> callables = new ArrayList<>(participants);

                for (BenchmarkMethodDescriptor methodDesc : benchDesc.testMethods) {
                    int threads = methodDesc.nThreads[i];
                    Callable<Long> benchmarkRunnable = () -> {
                        try {
                            // wait until all threads are ready
                            barrier.await();
                            long start = System.nanoTime();
                            methodDesc.method.invoke(testObject, nTimes, threads);
                            return System.nanoTime() - start;
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    };

                    for (int j = 0; j < threads; j++) {
                        callables.add(benchmarkRunnable);
                    }
                }

                // run all callables
                List<Future<Long>> results = svc.invokeAll(callables);
                // collect time of all callables and sum up
                long maxTime = 0l;
                for (Future<Long> result : results) {
                    long time = result.get();
                    if(time > maxTime) {
                        maxTime = time;
                    }

                }
                // TODO it is not clear how to calculate total time 
                // divide through nTimes
                maxTime = (long) (maxTime / (double) nTimes);
                System.out.println(
                        "- Run[" + i + "] " + String.join(", ", methodStr) + ", Duration: " + maxTime + "ns");
                svc.shutdownNow(); // terminate all threads
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class BenchmarkRunnable implements Runnable {
        private final CyclicBarrier barrier;

        public BenchmarkRunnable(CyclicBarrier barrier, int nTimes) {
            this.barrier = barrier;
        }

        @Override
        public void run() {
            try {
                barrier.await();

                barrier.await();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
