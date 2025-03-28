import java.util.concurrent.Semaphore;

public abstract class Process implements Runnable {

    Thread thread;
    Semaphore semaphore;
    boolean quantumExpired = false;

    public Process() {
        thread = new Thread(this);
        semaphore = new Semaphore(0);
        thread.start();
    }

    /**
     * Requests the process to stop by setting its quantum to expired.
     */
    public void requestStop() {
        quantumExpired = true;
    }

    /**
     * The process body.
     */
    public abstract void main();

    /**
     * Has the processes' execution been halted by the OS?
     * @return
     */
    public boolean isStopped() {
        return semaphore.availablePermits() == 0;
    }

    /**
     * The process is done if the thread has finished its execution.
     * @return Is the thread alive?
     */
    public boolean isDone() {
        return !thread.isAlive();
    }

    public void start() {
        System.out.println(String.format("Starting process %s", this));
        semaphore.release();
        System.out.println(String.format("Started process %s", this));
    }

    public void stop() {
        System.out.println(String.format("Stopping process %s", this));
        try {
            semaphore.acquire();
            System.out.println(String.format("Stopped process %s", this));
        } catch (InterruptedException e) {}
    }

    @Override
    public void run() { // This is called by the Thread - NEVER CALL THIS!!!
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {}
        main();
    }

    public void cooperate() {
        if (quantumExpired) {
            quantumExpired = false;
            OS.switchProcess();
        }
    }

    public boolean isQuantumExpired() {
        return quantumExpired;
    }
}
