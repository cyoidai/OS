import java.util.LinkedList;

/**
 * Process control block. Contains information about this process used by the
 * operating system. Most of which is not for individual process to access.
 * */
public class PCB {

    public final int pid;
    private static int nextPid = 1;
    private final Process process;
    private final String name;
    private OS.PriorityType priority;
    /** When the OS should wake up this process if it was put to sleep */
    private long wakeTime = 0;
    private int timeoutCounter = 0;
    public final int[] descriptors = {-1,-1,-1,-1};
    private final LinkedList<KernelMessage> messageQueue = new LinkedList<>();
    /** Contains mapping information for this process' virtual pages. Indices
     * are the virtual page addresses for this process and values contain either
     * a physical (memory) page address or disk page address. */
    private final Page[] pageTable = new Page[100];

    PCB(UserlandProcess up, OS.PriorityType priority) {
        process = up;
        name = up.getClass().getSimpleName();
        this.priority = priority;
        pid = nextPid;
        nextPid++;
//        Arrays.fill(pageTable, -1);
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

    public boolean isDone() {
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

    /**
     * Given a virtual page number, returns its mapping information.
     * Returns {@code null} on failure.
     * @param virtualPage Virtual page address to find.
     * @return Mapping information associated with {@code virtualPage}.
     */
    public Page getMapping(int virtualPage) {
        if (virtualPage < 0 || virtualPage >= pageTable.length)
            return null;
        return pageTable[virtualPage];
    }

    /**
     * Finds an empty region in the page table of length {@code size}, returning
     * the index of the first empty page and -1 if one cannot be found.
     * @param size Size of the allocation to look for, in number of pages.
     * @return Starting index of the empty region.
     */
    private int findEmptyVirtualPageRegion(int size) {
        for (int i = 0; i <= pageTable.length - size; i++) {
            boolean found = true;
            for (int j = 0; j < size; j++)
                if (pageTable[i + j] != null) {
                    found = false;
                    break;
                }
            if (found)
                return i;
        }
        return -1;
    }

    /**
     * Finds and creates a new page table mapping, returning the virtual address
     * that starts the allocation. If a mapping cannot be made (i.e. a
     * contiguous virtual mapping cannot be made), -1 is returned.
     * @param pages Array containing virtual address mappings. These mappings
     *              are provided by the operating system.
     * @return The virtual address of the start of the allocation.
     */
    public int allocateMemory(Page[] pages) {
        int start = findEmptyVirtualPageRegion(pages.length);
        if (start == -1)
            return -1;
        for (int i = 0; i < pages.length; i++)
            pageTable[start + i] = pages[i];
        return start * Hardware.PAGE_SIZE;
    }

    /**
     * Deletes mappings from the page table.
     * @param virtualPage Virtual page address to start at.
     * @param size Number of pages to delete.
     * @return Array containing the mappings that were deleted.
     */
    public Page[] freeMemory(int virtualPage, int size) {
        Page[] freedPages = new Page[size];
        for (int i = 0; i < size; i++) {
            freedPages[i] = pageTable[virtualPage + i];
            pageTable[virtualPage + i] = null;
        }
        return freedPages;
    }

    /**
     * Deletes all mappings from the page table.
     * @return Array containing the mappings that were deleted.
     */
    public Page[] freeAllMemory() {
        Page[] freedPages = new Page[pageTable.length];
        int next = 0;
        for (int i = 0; i < pageTable.length; i++) {
            if (pageTable[i] == null)
                continue;
            freedPages[next++] = pageTable[i];
            pageTable[i] = null;
        }
        return freedPages;
    }

    public Page[] getPageTable() {
        return pageTable;
    }

    @Override
    public String toString() {
        return process.toString();
    }
}
