
/**
 * Process control block
 */
public class PCB {

    public final int pid;
    private static int nextPid = 1;
    private OS.PriorityType priority;
    private final Process process;
    private long wakeTime = 0;
    private int timeoutCounter = 0;
    public final int[] descriptors = {-1,-1,-1,-1};

    PCB(UserlandProcess up, OS.PriorityType priority) {
        process = up;
        this.priority = priority;
        pid = nextPid;
        nextPid++;
    }

    public String getName() {
        return null;
    }

    public void requestStop() {
        process.requestStop();
    }

    public void stop() { /* calls userlandprocess’ stop. Loops with Thread.sleep() until
ulp.isStopped() is true.  */
        process.stop();
        while (!process.isStopped()) {
            try { Thread.sleep(30); }
            catch (InterruptedException e) {}
        }
    }

    public boolean isDone() { /* calls userlandprocess’ isDone() */
        return process.isDone();
    }

    void start() { /* calls userlandprocess’ start() */
        process.start();
    }

    public OS.PriorityType getPriority() {
        return priority;
    }

    public void setPriority(OS.PriorityType newPriority) {
        priority = newPriority;
    }

    public long getWakeTime() {
        return wakeTime;
    }

    public void setWakeTime(long newWakeTime) {
        wakeTime = newWakeTime;
    }

    public int getTimeoutCounter() {
        return timeoutCounter;
    }

    public void incrementTimeoutCounter() {
        timeoutCounter++;
    }

    public void resetTimeoutCounter() {
        timeoutCounter = 0;
    }

    public boolean ranToTimeout() {
        return process.isQuantumExpired();
    }
}
