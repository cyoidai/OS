import java.util.LinkedList;

/** Process control block */
public class PCB {

    private static int nextPid = 1;

    public final int pid;
    private final Process process;
    private final String name;
    private OS.PriorityType priority;
    private long wakeTime = 0;
    private int timeoutCounter = 0;
    public final int[] descriptors = {-1,-1,-1,-1};
    private final LinkedList<KernelMessage> messageQueue = new LinkedList<>();

    PCB(UserlandProcess up, OS.PriorityType priority) {
        process = up;
        name = up.getClass().getSimpleName();
        this.priority = priority;
        pid = nextPid;
        nextPid++;
    }

    public String getName() {
        return name;
    }

    public void requestStop() {
        process.requestStop();
    }

    public void stop() {
        // calls userlandprocess’ stop. Loops with Thread.sleep() until ulp.isStopped() is true.
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

    /**
     * Removes (dequeues) a {@code KernelMessage} from the process' message
     * queue. Returns {@code null} if the queue is empty.
     */
    public KernelMessage readMessage() {
        return messageQueue.poll();
    }

    /**
     * Adds (enqueues) a {@code KernelMessage} to the process' message queue.
     * @param msg message to enqueue
     */
    public void deliverMessage(KernelMessage msg) {
        messageQueue.add(msg);
    }

    @Override
    public String toString() {
        return process.toString();
    }
}
