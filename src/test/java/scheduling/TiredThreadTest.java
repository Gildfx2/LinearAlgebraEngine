package scheduling;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TiredThreadTest {

    /**
     * Test 1: Simple Fatigue Check
     * Verify that a worker starts with 0 fatigue,
     * and that the fatigue becomes greater than 0 after completing a task.
     */
    @Test
    void testFatigueIncreases() throws InterruptedException {
        // Create a worker with a fatigue factor of 10
        TiredThread worker = new TiredThread(1, 10.0);
        worker.start();

        // Check 1: Initially, the worker has done nothing, so fatigue should be 0
        assertEquals(0, worker.getFatigue(), "Fatigue should start at 0");

        // Submit a task that simulates time update (as an executor would do)
        worker.newTask(() -> {
            try {
                Thread.sleep(20);
                worker.addTimeUsed(20); // Simulating executor update
            } catch (InterruptedException e) {}
        });

        // Wait a moment for the thread to finish the task
        Thread.sleep(100);

        // Check 2: Now the worker has worked, so fatigue must be positive (> 0)
        assertTrue(worker.getFatigue() > 0, "Fatigue should increase after working");

        worker.shutdown();
    }


    /**
     * Test 2: Queue Capacity (Overflow)
     * The queue is defined with size 1. We try to insert 2 tasks when the thread isn't pulling them.
     * Expectation: The second task should throw IllegalStateException.
     */
    @Test
    void testTaskOverflow() {
        TiredThread worker = new TiredThread(1, 1.0);

        // TRICK: We do NOT call start().
        // This ensures the thread doesn't pull from the queue, so it remains full after the first task.

        // 1. First task - should succeed
        assertDoesNotThrow(() -> worker.newTask(() -> {}));

        // 2. Second task - should fail because queue (size 1) is full
        assertThrows(IllegalStateException.class, () -> {
            worker.newTask(() -> {});
        }, "Should throw exception when queue is full");
    }

    /**
     * Test 3: Comparable Implementation
     * Verifies that compareTo works based on Fatigue values.
     */
    @Test
    void testCompareTo() {
        TiredThread t1 = new TiredThread(1, 1.0); // Normal worker
        TiredThread t2 = new TiredThread(2, 10.0); // Worker that gets tired very fast

        t1.start();
        t2.start();

        // Tasks include manual time updates to simulate executor behavior
        t1.newTask(() -> t1.addTimeUsed(10));
        t2.newTask(() -> t2.addTimeUsed(10));

        // Wait for them to finish
        try { Thread.sleep(50); } catch (InterruptedException e) {}

        // t2 should be more fatigued than t1 because its factor is 10
        assertTrue(t2.getFatigue() > t1.getFatigue());

        // Therefore, t2 should be 'greater' than t1 in comparison
        assertTrue(t2.compareTo(t1) > 0, "Thread with higher fatigue should be 'greater'");

        t1.shutdown();
        t2.shutdown();
    }

    /**
     * Test 4: Graceful Shutdown
     * Verifies that the thread actually stops running after shutdown() is called.
     */
    @Test
    void testShutdown() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);
        worker.start();

        assertTrue(worker.isAlive(), "Worker should be alive initially");

        worker.shutdown();

        // Wait for thread to die
        worker.join(1000); // 1 second timeout

        assertFalse(worker.isAlive(), "Worker should be dead after shutdown");
    }


    /**
     * Test 5: Idle Time Accumulation
     * Checks that idle time increases when the thread is waiting for tasks.
     */
    @Test
    void testIdleTimeAccumulation() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);
        worker.start();

        // Let the thread wait for a bit without tasks
        Thread.sleep(50);

        // Update idle time manually to simulate executor behavior
        worker.addTimeIdle(50);

        assertTrue(worker.getTimeIdle() >= 50, "Idle time should accumulate when waiting for tasks");

        // Verify that idleStartTime was updated in the thread's run() loop after a task
        long firstIdle = worker.getIdleStartTime();
        worker.newTask(() -> {});
        Thread.sleep(20);

        assertTrue(worker.getIdleStartTime() > firstIdle, "idleStartTime should reset after a task");

        worker.shutdown();
    }

    /**
     * Test 6: Null Task Handling
     * Verifies that the worker rejects null tasks, usually throwing NullPointerException
     * (standard behavior for BlockingQueue).
     */
    @Test
    void testNullTask() {
        TiredThread worker = new TiredThread(1, 1.0);

        // Expect standard NullPointerException from BlockingQueue
        assertThrows(NullPointerException.class, () -> {
            worker.newTask(null);
        }, "Should not accept null tasks");
    }

}