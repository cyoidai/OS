
import java.time.Clock;
import java.util.*;

public class Scheduler {

    public PCB currentlyRunning = null;

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

    private final Timer  timer = new Timer();
    private final Clock  clock = Clock.systemDefaultZone();
    private final Random rng   = new Random();
    private final VirtualFileSystem vfs;

    /**
     * @param vfs Required in order to close devices on process destruction.
     */
    public Scheduler(VirtualFileSystem vfs) {
        this.vfs = vfs;
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (currentlyRunning != null)
                    currentlyRunning.requestStop();
//                for (PCB pcb : realtimeQueue)
//                    pcb.requestStop();
//                for (PCB pcb : interactiveQueue)
//                    pcb.requestStop();
//                for (PCB pcb : backgroundQueue)
//                    pcb.requestStop();
            }
        };
        timer.schedule(task, 0, 250);
    }

    /**
     * Creates a new PCB container for a userland process and adds it to the
     * scheduler. If no process is currently running, the newly added process
     * will be set to run next.
     * @param up the process to add
     * @param p  the process' priority
     * @return
     */
    public int CreateProcess(UserlandProcess up, OS.PriorityType p) {
        switch (p) {
            case OS.PriorityType.interactive:
                interactiveQueue.add(new PCB(up, p));
                break;
            case OS.PriorityType.background:
                backgroundQueue.add(new PCB(up, p));
                break;
            case OS.PriorityType.realtime:
                realtimeQueue.add(new PCB(up, p));
                break;
        }
        if (currentlyRunning == null)
            SwitchProcess();
        return 0;
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
            return;
        if (currentlyRunning != null && currentlyRunning.isDone())
            DestroyProcess();
        else if (deschedule)
            DestroyProcess();
        else
            RequeueRunningProcess();
        currentlyRunning = nextProcess;
    }

    private PCB GetNextProcess() {
        PCB nextProcess = null;

        if (!sleepingQueue.isEmpty())
            if (clock.millis() >= sleepingQueue.peek().getWakeTime())
                return sleepingQueue.remove();

        int realtimeWeight;
        int interactiveWeight;
        int backgroundWeight;

        if (!realtimeQueue.isEmpty()) {
            realtimeWeight    = 60;
            interactiveWeight = 30;
            backgroundWeight  = 10;
        } else if (!interactiveQueue.isEmpty()) {
            realtimeWeight    = 0;
            interactiveWeight = 75;
            backgroundWeight  = 25;
        } else {
            realtimeWeight    = 0;
            interactiveWeight = 75;
            backgroundWeight  = 25;
        }

        int randint = rng.nextInt(realtimeWeight + interactiveWeight + backgroundWeight);
        OS.PriorityType nextQueue;
        if (randint < realtimeWeight)
            nextQueue = OS.PriorityType.realtime;
        else if (randint < realtimeWeight + interactiveWeight)
            nextQueue = OS.PriorityType.interactive;
        else
            nextQueue = OS.PriorityType.background;

        // Fallthrough if there's no processes in whatever queue got selected
        switch (nextQueue) {
            case realtime:
                if (!realtimeQueue.isEmpty()) {
                    nextProcess = realtimeQueue.removeFirst();
                    break;
                }
            case interactive:
                if (!interactiveQueue.isEmpty()) {
                    nextProcess = interactiveQueue.removeFirst();
                    break;
                }
            case background:
                if (!backgroundQueue.isEmpty()) {
                    nextProcess = backgroundQueue.removeFirst();
                    break;
                }
        }
        return nextProcess;
    }

//    /**
//     * Updates {@code Scheduler.currentlyRunning} with a new process to run from
//     * the process queues.
//     * @param deschedule When true, the currently running process will be
//     *                   dropped from the scheduler and will never run again.
//     *                   When false, functions the same as
//     *                   {@code Scheduler.SwitchProcess()}.
//     */
//    public void SwitchProcess(boolean deschedule) {
//        if (currentlyRunning != null && currentlyRunning.isDone())
//            DestroyProcess();
//        if (deschedule)
//            DestroyProcess();
//
//        if (!sleepingQueue.isEmpty())
//            if (clock.millis() >= sleepingQueue.peek().getWakeTime()) {
//                requeueRunningProcess();
//                currentlyRunning = sleepingQueue.remove();
//                return;
//            }
//
//        int realtimeWeight;
//        int interactiveWeight;
//        int backgroundWeight;
//
//        if (!realtimeQueue.isEmpty()) {
//            realtimeWeight    = 60;
//            interactiveWeight = 30;
//            backgroundWeight  = 10;
//        } else if (!interactiveQueue.isEmpty()) {
//            realtimeWeight    = 0;
//            interactiveWeight = 75;
//            backgroundWeight  = 25;
//        } else if (!backgroundQueue.isEmpty()) {
//            // Only background processes exist
//            requeueRunningProcess();
//            currentlyRunning = backgroundQueue.removeFirst();
//            return;
//        } else {
//            // All queues are empty, just do nothing
//            return;
//        }
//
//        int randint = rng.nextInt(realtimeWeight + interactiveWeight + backgroundWeight);
//        OS.PriorityType nextQueue;
//        if (randint < realtimeWeight)
//            nextQueue = OS.PriorityType.realtime;
//        else if (randint < realtimeWeight + interactiveWeight)
//            nextQueue = OS.PriorityType.interactive;
//        else
//            nextQueue = OS.PriorityType.background;
//
//        // Fallthrough if there's no processes in whatever queue got selected
//        switch (nextQueue) {
//            case OS.PriorityType.realtime:
//                if (!realtimeQueue.isEmpty()) {
//                    requeueRunningProcess();
//                    currentlyRunning = realtimeQueue.removeFirst();
//                    break;
//                }
//            case OS.PriorityType.interactive:
//                if (!interactiveQueue.isEmpty()) {
//                    requeueRunningProcess();
//                    currentlyRunning = interactiveQueue.removeFirst();
//                    break;
//                }
//            case OS.PriorityType.background:
//                if (!backgroundQueue.isEmpty()) {
//                    requeueRunningProcess();
//                    currentlyRunning = backgroundQueue.removeFirst();
//                    break;
//                }
//        }
//    }

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

    public void Sleep(int ms) {
        currentlyRunning.resetTimeoutCounter();
        currentlyRunning.setWakeTime(clock.millis() + ms);
        sleepingQueue.add(currentlyRunning);
        currentlyRunning = null;
        SwitchProcess();
    }

    public void DestroyProcess() {
        if (currentlyRunning == null)
            return;

        for (int id : currentlyRunning.descriptors)
            if (id != -1)
                vfs.Close(id);
    }

    public PCB GetCurrentlyRunning() {
        return currentlyRunning;
    }

    public void printState() {
        System.out.println(String.format("Running: %s", null));
        System.out.println(String.format("RT %s", realtimeQueue));
        System.out.println(String.format("IT %s", interactiveQueue));
        System.out.println(String.format("BG %s", backgroundQueue));
        System.out.println(String.format("Sleep %s", sleepingQueue));
    }
}
