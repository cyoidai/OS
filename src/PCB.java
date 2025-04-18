import java.util.Arrays;
import java.util.LinkedList;

/** Process control block */
public class PCB {

    public final int pid;
    private static int nextPid = 1;
    private final Process process;
    private final String name;
    private OS.PriorityType priority;
    private long wakeTime = 0;
    private int timeoutCounter = 0;
    public final int[] descriptors = {-1,-1,-1,-1};
    private final LinkedList<KernelMessage> messageQueue = new LinkedList<>();
    /** Indices are virtual page addresses and values are physical page addresses */
    private final int[] pageTable = new int[100];

    PCB(UserlandProcess up, OS.PriorityType priority) {
        process = up;
        name = up.getClass().getSimpleName();
        this.priority = priority;
        pid = nextPid;
        nextPid++;
        Arrays.fill(pageTable, -1);
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
     * Given a virtual page number, returns the physical address for that page.
     * Returns -1 on failure.
     * @param virtualPage Virtual page address to find.
     * @return The physical page address associated with {@code virtualPage}.
     */
    public int getMapping(int virtualPage) {
        if (virtualPage < 0 || virtualPage >= pageTable.length)
            return -1;
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
                if (pageTable[i + j] != -1) {
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
     * @param physicalPageAddresses Array of physical page addresses to assign
     *                              each virtual page allocation.
     * @return The virtual address of the start of the allocation.
     */
    public int allocateMemory(int[] physicalPageAddresses) {
        int start = findEmptyVirtualPageRegion(physicalPageAddresses.length);
        if (start == -1)
            return -1;
        int i = start;
        for (int physicalAddress : physicalPageAddresses) {
            pageTable[i] = physicalAddress;
            i++;
        }
        return start * Hardware.PAGE_SIZE;
    }

    /**
     * Deletes mappings from the page table.
     * @param virtualPage Virtual page address.
     * @param size Number of pages to delete.
     * @return Array of the physical page addresses that were deleted.
     */
    public int[] freeMemory(int virtualPage, int size) {
        int[] physicalPages = new int[size];
        for (int i = 0; i < size; i++) {
            physicalPages[i] = pageTable[virtualPage + i];
            pageTable[virtualPage + i] = -1;
        }
        return physicalPages;
    }

    public int[] getPageTable() {
        return pageTable;
    }

    @Override
    public String toString() {
        return process.toString();
    }
}
