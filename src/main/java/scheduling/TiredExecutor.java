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
        workers = new TiredThread[numThreads];// placeholder
        for(int i=0;i<numThreads;i++){
            double fatigue = Math.random() + 0.5;
            TiredThread thread = new TiredThread(i, fatigue);
            workers[i] = thread;
            idleMinHeap.add(thread);
            thread.start();
        }

    }

    public void submit(Runnable task) {
        // TODO
        try {
            inFlight.incrementAndGet();
            TiredThread threadToSubmit = idleMinHeap.take();
            Runnable wrappedTask = () -> {
                try{
                    task.run();
                }
                finally{
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
        // TODO: submit tasks one by one and wait until all finish
            int i =0;
            for (Runnable task : tasks) {
                submit(task);
                i++;
            }
            // Lock the AtomicInteger itself to wait\
            synchronized (inFlight) {
                while (i >0) {
                    try {
                        inFlight.wait(); // This works!
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    i--;
                }
            }
    }

    public void shutdown() throws InterruptedException {
        // TODO
        for(TiredThread worker: workers){
            worker.shutdown();
            if (Thread.interrupted())
                throw new InterruptedException();
        }
    }

    public synchronized String getWorkerReport() {
        // TODO: return readable statistics for each worker
        StringBuilder reports = new StringBuilder();
        for(TiredThread worker : workers){
            String report = "Worker " + worker.getWorkerId() + ":\tFatigue: " + worker.getFatigue() +
                    "\tTime used: " + worker.getTimeUsed() + "\tTime idle: " + worker.getTimeIdle();
            reports.append(report).append("\n");
        }
        return reports.toString();
    }


}
