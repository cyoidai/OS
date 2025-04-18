
import java.time.Clock;
import java.util.*;

public class Scheduler {

    public PCB currentlyRunning;

    private final HashMap<Integer, PCB> pcbByPID = new HashMap<>();
    private final HashMap<String, PCB> pcbByName = new HashMap<>();

    // process queues
    private final LinkedList<PCB> realtimeQueue    = new LinkedList<>();
    private final LinkedList<PCB> interactiveQueue = new LinkedList<>();
    private final LinkedList<PCB> backgroundQueue  = new LinkedList<>();
    private final PriorityQueue<PCB> sleepingQueue = new PriorityQueue<>(
        new Comparator<PCB>() {
            @Override
            public int compare(PCB o1, PCB o2) {
                if (o1.getWakeTime() == o2.getWakeTime())
                    return 0;
                return o1.getWakeTime() < o2.getWakeTime() ? -1 : 1;
            }
        }
    );
    private final ArrayList<PCB> awaitingMessage = new ArrayList<>();

    private final Timer  timer = new Timer();
    private final Clock  clock = Clock.systemDefaultZone();
    private final Random rng   = new Random();
    private final VirtualFileSystem vfs;

    /**
     * @param init The process scheduler is initialized with. Is usually the
     *             process to launch all other processes and should never exit.
     * @param vfs Reference to the systems VFS in order to properly close any
     *            open devices upon process destruction.
     */
    public Scheduler(UserlandProcess init, VirtualFileSystem vfs) {
//        this.currentlyRunning = new PCB(new IdleProcess(), OS.PriorityType.background);
        currentlyRunning = new PCB(init, OS.PriorityType.interactive);
        CreateProcess(new IdleProcess(), OS.PriorityType.background);
        this.vfs = vfs;
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (currentlyRunning != null)
                    currentlyRunning.requestStop();
            }
        };
        timer.schedule(task, 0, 250);
        currentlyRunning.start();
    }

    /**
     * Creates a new PCB container for a userland process and adds it to the
     * scheduler.
     * @param up the process to add
     * @param p  the process' priority
     * @return Process ID of the newly created process
     */
    public int CreateProcess(UserlandProcess up, OS.PriorityType p) {
        PCB pcb = new PCB(up, p);
        switch (p) {
            case OS.PriorityType.interactive -> interactiveQueue.add(pcb);
            case OS.PriorityType.background  -> backgroundQueue.add(pcb);
            case OS.PriorityType.realtime    -> realtimeQueue.add(pcb);
        }
        pcbByPID.put(pcb.pid, pcb);
        pcbByName.put(pcb.getName(), pcb);
        return pcb.pid;
    }

    /**
     * Reschedules the currently running process for later and updates
     * {@code Scheduler.currentlyRunning} with a new process to run from the
     * process queues.
     */
    public void SwitchProcess() {
        SwitchProcess(false);
    }

    /**
     * Updates {@code Scheduler.currentlyRunning} with a new process to run from
     * the process queues.
     * @param deschedule When true, the currently running process will be
     *                   dropped from the scheduler and will never run again.
     *                   When false, functions the same as
     *                   {@code Scheduler.SwitchProcess()}.
     */
    public void SwitchProcess(boolean deschedule) {
        PCB nextProcess = GetNextProcess();
        if (nextProcess == null)
            // no processes to run, just continue running whatever is currently
            // running
            return;
        if (currentlyRunning != null) {
            if (currentlyRunning.isDone())
                // process execution ended unexpectedly
                DestroyRunningProcess();
            else if (deschedule)
                // program called OS.Exit()
                DestroyRunningProcess();
        } else
            RequeueRunningProcess();
        currentlyRunning = nextProcess;
    }

    /**
     * Fetches the next process ready for execution. Removes it from its queue,
     * but does not update {@code currentlyRunning}. Returns {@code null} if
     * there are no processes to switch to.
     * @return Next process to run.
     */
    private PCB GetNextProcess() {

        if (!sleepingQueue.isEmpty())
            if (clock.millis() >= sleepingQueue.peek().getWakeTime())
                return sleepingQueue.remove();

        if (!awaitingMessage.isEmpty()) {
            for (int i = 0; i < awaitingMessage.size(); i++) {
                PCB pcb = awaitingMessage.get(i);
                KernelMessage msg = pcb.readMessage();
                if (msg == null)
                    continue;
                OS.retVal = msg;
                awaitingMessage.remove(i);
                return pcb;
            }
        }

        int randint = rng.nextInt(100);
        if (!realtimeQueue.isEmpty()) {
            if (!backgroundQueue.isEmpty() && randint < 10)
                return backgroundQueue.remove();
            if (!interactiveQueue.isEmpty() && randint < 30)
                return interactiveQueue.remove();
            return realtimeQueue.remove();
        }
        if (!interactiveQueue.isEmpty()) {
            if (!backgroundQueue.isEmpty() && randint < 25)
                return backgroundQueue.remove();
            return interactiveQueue.remove();
        }
        if (!backgroundQueue.isEmpty())
            return backgroundQueue.remove();
        return null;
    }

    /**
     * Requeue (add) the currently running process back into its proper queue
     * and clear {@code currentlyRunning}. Also handles process priority
     * demotion based on its timeout counter (times the process has run to its
     * timeout).
     */
    private void RequeueRunningProcess() {
        if (currentlyRunning == null)
            return;

        // process priority demotion
        if (currentlyRunning.getPriority() != OS.PriorityType.background) {
            if (currentlyRunning.ranToTimeout())
                currentlyRunning.incrementTimeoutCounter();
            else
                currentlyRunning.resetTimeoutCounter();
            if (currentlyRunning.getTimeoutCounter() > 5)
                switch (currentlyRunning.getPriority()) {
                    case realtime    -> currentlyRunning.setPriority(OS.PriorityType.interactive);
                    case interactive -> currentlyRunning.setPriority(OS.PriorityType.background);
                }
        }

        switch (currentlyRunning.getPriority()) {
            case realtime    -> realtimeQueue.add(currentlyRunning);
            case interactive -> interactiveQueue.add(currentlyRunning);
            case background  -> backgroundQueue.add(currentlyRunning);
        }
        currentlyRunning = null;
    }

    /**
     * Puts the currently running process to sleep.
     * @param ms minimum duration to sleep for, in milliseconds.
     */
    public void Sleep(int ms) {
        currentlyRunning.resetTimeoutCounter();
        currentlyRunning.setWakeTime(clock.millis() + ms);
        sleepingQueue.add(currentlyRunning);
        currentlyRunning = null;
        SwitchProcess();
    }

    /**
     * Checks if the currently running process has a {@code KernelMessage} in
     * its message queue and returns it. Otherwise, the process is put into a
     * waiting state until it receives one and a new process will run.
     * @return The message, if there is one. Otherwise, {@code null} and the
     * process is placed into a waiting state.
     */
    public KernelMessage AwaitMessage() {
        KernelMessage msg = currentlyRunning.readMessage();
        if (msg != null)
            return msg;
        awaitingMessage.add(currentlyRunning);
        currentlyRunning = null;
        SwitchProcess();
        return null;
    }

    /**
     * Destroys whatever process is currently running.
     */
    private void DestroyRunningProcess() {
        pcbByName.remove(currentlyRunning.getClass().getSimpleName());
        pcbByPID.remove(currentlyRunning.pid);
        for (int id : currentlyRunning.descriptors)
            if (id != -1)
                vfs.Close(id);
    }

    /**
     * Delivers a {@code KernelMessage} to its associated process. Fails
     * saliently if the process cannot be found.
     * @param msg message to deliver
     */
    public void DeliverMessage(KernelMessage msg) {
        PCB targetPCB = pcbByPID.get(msg.getTargetPID());
        if (targetPCB == null)
            return;
        targetPCB.deliverMessage(msg);
    }

    public PCB GetCurrentlyRunning() {
        return currentlyRunning;
    }

    /**
     * Returns the process ID (PID) of a process stored within this scheduler by
     * its name.
     * @param name The process' name to query.
     * @return The process' PID. -1 on failure.
     */
    public int GetPIDByName(String name) {
        if (!pcbByName.containsKey(name))
            return -1;
        return pcbByName.get(name).pid;
    }

    @Override
    public String toString() {
        return String.format("""
                Running: %s
                RT %s
                IT %s
                BG %s
                Sleep %s""", currentlyRunning, realtimeQueue, interactiveQueue, backgroundQueue, sleepingQueue);
//        System.out.println(String.format("Running: %s", null));
//        System.out.println(String.format("RT %s", realtimeQueue));
//        System.out.println(String.format("IT %s", interactiveQueue));
//        System.out.println(String.format("BG %s", backgroundQueue));
//        System.out.println(String.format("Sleep %s", sleepingQueue));
    }
}
