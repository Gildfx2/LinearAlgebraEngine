package scheduling;

import java.util.Random;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TiredExecutor {

    private final TiredThread[] workers;
    private final PriorityBlockingQueue<TiredThread> idleMinHeap = new PriorityBlockingQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger(0);


    public TiredExecutor(int numThreads) {
        // TODO
        workers = new TiredThread[numThreads];
        for(int i=0;i<numThreads;i++){
            double fatigueFactor = Math.random() + 0.5; // Generating a random value between 0.5 to 1.5 representing the fatigue factor
            TiredThread thread = new TiredThread(i, fatigueFactor);
            workers[i] = thread;
            idleMinHeap.add(thread); // At first, all of the threads are idle
            thread.start();
        }

    }

    public void submit(Runnable task) {
        // TODO
        try {
            inFlight.incrementAndGet();
            TiredThread threadToSubmit = idleMinHeap.take(); // take will retrieve and remove the head of this queue, waiting if necessary until an element becomes available.
            // the wrapped task is to make sure that the inFlight and idleMinHeap are updated after the thread finishes the task.
            // also after the thread finishes the task we want to notify the other threads (in case inFlight=0) because it means that all of the tasks completed
            Runnable wrappedTask = () -> {
                long startWorkTime = System.nanoTime();
                threadToSubmit.addTimeIdle(startWorkTime - threadToSubmit.getIdleStartTime());
                try{
                    task.run();
                }
                finally{
                    threadToSubmit.addTimeUsed(System.nanoTime()- startWorkTime);
                    inFlight.decrementAndGet();
                    idleMinHeap.add(threadToSubmit);
                    if(inFlight.get() ==0) {
                        synchronized (inFlight) {
                            inFlight.notifyAll();
                        }
                    }

                }
            };
            threadToSubmit.newTask(wrappedTask);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }

    public void submitAll(Iterable<Runnable> tasks) {
        for (Runnable task : tasks) {
            submit(task);
        }
        synchronized (inFlight) {
            while (inFlight.get() > 0) { // Checking if there is some busy workers, because we need to wait until all of the tasks are finished
                try {
                    inFlight.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void shutdown() throws InterruptedException {
        // TODO
        for(TiredThread worker: workers){
            worker.shutdown(); // shutdown the worker
            if (Thread.interrupted())
                throw new InterruptedException();
        }
    }

    public synchronized String getWorkerReport() {
        // TODO: return readable statistics for each worker
        StringBuilder reports = new StringBuilder();

        for (TiredThread worker : workers) {
            long usedMs = worker.getTimeUsed();
            long idleMs = worker.getTimeIdle();

            String report = "Worker " + worker.getWorkerId() +
                    ":\tFatigue: " + worker.getFatigue() +
                    "\tTime used: " + usedMs +
                    "\tTime idle: " + idleMs;

            reports.append(report).append("\n");
        }

        reports.append("----------------------------------------\n");
        reports.append("Fairness Score (Lower is better): ")
                .append(String.format("%.4f", getFairnessScore()));

        return reports.toString();
    }

    /**
     * @return the fairness score of the workers
     */
    private double getFairnessScore() {
        // Calculate fatigue average
        double totalFatigue = 0;
        for (TiredThread worker : workers) {
            totalFatigue += worker.getFatigue();
        }
        double averageFatigue = totalFatigue / workers.length;

        // Calculating the time of squared deviations from the average
        double sumSquaredDeviations = 0;
        for (TiredThread worker : workers) {
            double deviation = worker.getFatigue() - averageFatigue;
            sumSquaredDeviations += (deviation * deviation);
        }

        return sumSquaredDeviations;
    }


}