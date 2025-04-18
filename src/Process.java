import java.util.concurrent.Semaphore;

public abstract class Process implements Runnable {

    Thread thread;
    Semaphore semaphore;
    boolean quantumExpired = false;

    public Process() {
        thread = new Thread(this, this.getClass().getSimpleName());
        semaphore = new Semaphore(0);
        thread.start();
    }

    /**
     * Requests the process to stop by setting its quantum to "expired".
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
        semaphore.release();
//        System.out.println(String.format("Started process %s", this));
    }

    public void stop() {
//        System.out.println(String.format("Stopping process %s", this));
        try {
            semaphore.acquire();
//            System.out.println(String.format("Stopped process %s", this));
        } catch (InterruptedException e) {}
    }

    @Override
    public void run() { // This is called by the Thread - NEVER CALL THIS!!!
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {}
        try {
            main();
        } catch (Exception e) {
            System.out.println(String.format("Process '%s' crashed unexpectedly", this));
            e.printStackTrace();
            OS.Exit();
        }
        OS.Exit();
    }

    public void cooperate() {
        if (quantumExpired) {
            quantumExpired = false;
            OS.SwitchProcess();
        }
    }

    public boolean isQuantumExpired() {
        return quantumExpired;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
