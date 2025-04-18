import java.util.Arrays;

public class Kernel extends Process implements Device {

    private final VirtualFileSystem vfs = new VirtualFileSystem();
    private Scheduler scheduler = null;
    /** Array where indices are page indices ({@code address / PAGE_SIZE}), and
     * values indicate whether that page is in use ({@code true}) or not in use
     * ({@code false}). */
    private final boolean[] freePages = new boolean[Hardware.MEM_SIZE / Hardware.PAGE_SIZE];

    public Kernel(UserlandProcess init) {
        super();
        Hardware.clearTLB();
        scheduler = new Scheduler(init, vfs);
    }

    @Override
    public void main() {
        while (scheduler == null) ;
        while (scheduler.currentlyRunning == null) ;
            // Prevents the kernel from executing anything until scheduler has
            // been initialized with a process ready to run. Otherwise,
            // `scheduler.currentlyRunning.start()` will fail. This is because
            // thread execution starts in the superclass.
        while (true) {
            switch (OS.currentCall) { // get a job from OS, do it
                case CreateProcess ->  // Note how we get parameters from OS and set the return value
                        OS.retVal = CreateProcess((UserlandProcess) OS.parameters.get(0), (OS.PriorityType) OS.parameters.get(1));
                case SwitchProcess -> SwitchProcess();
                // Priority scheduler
                case Sleep  -> Sleep((int)OS.parameters.get(0));
                case GetPID -> OS.retVal = GetPid();
                case Exit   -> Exit();
                // Devices
                case Open  -> OS.retVal = Open((String)OS.parameters.get(0));
                case Close -> Close((int)OS.parameters.get(0));
                case Read  -> OS.retVal = Read((int)OS.parameters.get(0), (int)OS.parameters.get(1));
                case Seek  -> Seek((int)OS.parameters.get(0), (int)OS.parameters.get(1));
                case Write -> OS.retVal = Write((int)OS.parameters.get(0), (byte[])OS.parameters.get(1));
                // Messages
                case GetPIDByName   -> OS.retVal = GetPidByName((String)OS.parameters.get(0));
                case SendMessage    -> SendMessage((KernelMessage)OS.parameters.get(0));
                case WaitForMessage -> WaitForMessage();
                // Memory
                case GetMapping     -> GetMapping((int)OS.parameters.get(0));
                case AllocateMemory -> OS.retVal = AllocateMemory((int)OS.parameters.get(0));
                case FreeMemory     -> OS.retVal = FreeMemory((int)OS.parameters.get(0), (int)OS.parameters.get(1));
            }
            scheduler.currentlyRunning.start();
            stop();
        }
    }

    /**
     * Starts the kernel, stopping whatever process is currently running.
     */
    @Override
    public void start() {
        super.start();
        scheduler.currentlyRunning.stop();
    }

    private void SwitchProcess() {
        scheduler.SwitchProcess();
        Hardware.clearTLB();
    }

    private int CreateProcess(UserlandProcess up, OS.PriorityType priority) {
        return scheduler.CreateProcess(up, priority);
    }

    private void Sleep(int mills) {
        scheduler.Sleep(mills);
    }

    private void Exit() {
        FreeAllMemory();
        scheduler.SwitchProcess(true);
    }

    private int GetPid() {
        return scheduler.currentlyRunning.pid;
    }

    private int GetPidByName(String name) {
        return scheduler.GetPIDByName(name);
    }

    public int Open(String s) {
        PCB p = scheduler.GetCurrentlyRunning();
        for (int i = 0; i < p.descriptors.length; i++)
            if (p.descriptors[i] == -1) {
                p.descriptors[i] = vfs.Open(s);
                return i;
            }
        return -1;
    }

    public void Close(int id) {
        PCB p = scheduler.GetCurrentlyRunning();
        int vfsID = p.descriptors[id];
        if (vfsID == -1)
            return;
        vfs.Close(vfsID);
        p.descriptors[id] = -1;
    }

    public byte[] Read(int id, int size) {
        PCB p = scheduler.GetCurrentlyRunning();
        int vfsID = p.descriptors[id];
        return vfs.Read(vfsID, size);
    }

    public void Seek(int id, int to) {
        PCB p = scheduler.GetCurrentlyRunning();
        int vfsID = p.descriptors[id];
        vfs.Seek(vfsID, to);
    }

    public int Write(int id, byte[] data) {
        PCB p = scheduler.GetCurrentlyRunning();
        int vfsID = p.descriptors[id];
        return vfs.Write(vfsID, data);
    }

    private void SendMessage(KernelMessage msg) {
        KernelMessage msgCopy = new KernelMessage(msg);
        msgCopy.setSenderPID(scheduler.currentlyRunning.pid);
        scheduler.DeliverMessage(msgCopy);
    }

    private void WaitForMessage() {
        KernelMessage msg = scheduler.AwaitMessage();
        if (msg != null)
            OS.retVal = msg;
    }

    private void GetMapping(int virtualPage) {
        int physicalAddress = scheduler.GetCurrentlyRunning().getMapping(virtualPage);
        if (physicalAddress != -1)
            Hardware.updateTLB(virtualPage, physicalAddress);
    }

    private int AllocateMemory(int size) {
        if (size % 1024 != 0)
            return -1;
        int pages = size / 1024;
        int[] physicalPageAddresses = new int[pages];
        Arrays.fill(physicalPageAddresses, -1);
        // search for unallocated memory blocks
        int pageIndex = 0;
        for (int i = 0; i < freePages.length; i++)
            if (!freePages[i]) {
                physicalPageAddresses[pageIndex] = i;
                if (pageIndex == pages - 1)
                    break;
                pageIndex++;
            }
        // make sure all pages were allocated
        if (pageIndex < physicalPageAddresses.length - 1)
            return -1;
        // now mark those pages in use
        for (int i = 0; i < physicalPageAddresses.length; i++)
            freePages[physicalPageAddresses[i]] = true;
        return scheduler.GetCurrentlyRunning().allocateMemory(physicalPageAddresses);
    }

    private boolean FreeMemory(int pointer, int size) {
        if (size % 1024 != 0 || pointer % 1024 != 0)
            return false;
        int[] physicalPages = scheduler.GetCurrentlyRunning().freeMemory(pointer / Hardware.PAGE_SIZE, size / Hardware.PAGE_SIZE);
        for (int physicalPage : physicalPages)
            freePages[physicalPage] = false;
        return true;
    }

    private void FreeAllMemory() {
        for (int physicalPage : scheduler.GetCurrentlyRunning().getPageTable())
            if (physicalPage != -1)
                freePages[physicalPage] = false;
    }
}
