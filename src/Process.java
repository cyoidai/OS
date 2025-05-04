import java.util.concurrent.Semaphore;

public abstract class Process implements Runnable {

    private final Thread thread;
    private final Semaphore semaphore;
    private boolean quantumExpired = false;

    public Process() {
        thread = new Thread(this, this.getClass().getSimpleName());
        semaphore = new Semaphore(0);
        thread.start();
    }

    /**
     * Requests the process to stop by setting its quantum to "expired".
     */
    public final void requestStop() {
        quantumExpired = true;
    }

    /**
     * The process body.
     */
    public abstract void main();

    /**
     * Has the processes' execution been halted by the OS?
     */
    public final boolean isStopped() {
        return semaphore.availablePermits() == 0;
    }

    /**
     * The process is done if the thread has finished its execution.
     * @return Is the thread alive?
     */
    public final boolean isDone() {
        return !thread.isAlive();
    }

    public void start() {
        semaphore.release();
    }

    public final void stop() {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {}
    }

    /**
     * This is called by the thread upon instantiation and thus should never be
     * called.
     */
    @Override
    public final void run() { // This is called by the Thread - NEVER CALL THIS!!!
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {}
        try {
            main();
        } catch (Exception e) {
            System.out.println(String.format("Process '%s' crashed unexpectedly", this));
            e.printStackTrace();
        } finally {
            onCrash();
        }
    }

    /**
     * This is only here because it will be overridden by the kernel. This is
     * because the kernel should not call {@code OS.Exit()} when it crashes,
     * but all other processes should.
     */
    public void onCrash() {
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
