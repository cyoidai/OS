
public class Kernel extends Process implements Device {

    private final VirtualFileSystem vfs = new VirtualFileSystem();
    private final Scheduler scheduler = new Scheduler(vfs);

    @Override
    public void main() {
        while (true) { // Warning on infinite loop is OK...
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
                case SendMessage    -> SendMessage();
                case WaitForMessage -> WaitForMessage();
                // Memory
                case GetMapping     -> GetMapping((int)OS.parameters.get(0));
                case AllocateMemory -> OS.retVal = AllocateMemory((int)OS.parameters.get(0));
                case FreeMemory     -> OS.retVal = FreeMemory((int)OS.parameters.get(0), (int)OS.parameters.get(1));
            }
            // TODO: Now that we have done the work asked of us, start some process then go to sleep.
            scheduler.SwitchProcess();
            scheduler.currentlyRunning.start();
//            if (scheduler.currentlyRunning != null)
//                scheduler.currentlyRunning.start();
            stop();
        }
    }

    /**
     * Starts the kernel, stopping whatever process is currently running.
     */
    @Override
    public void start() {
        super.start();
        if (scheduler.currentlyRunning != null)
            scheduler.currentlyRunning.stop();
    }

//    @Override
//    public void run() {
//        super.run();
//        main();
//    }

    private void SwitchProcess() {
        scheduler.SwitchProcess();
    }

    // For assignment 1, you can ignore the priority. We will use that in assignment 2
    private int CreateProcess(UserlandProcess up, OS.PriorityType priority) {
        return scheduler.CreateProcess(up, priority);
    }

    private void Sleep(int mills) {
        scheduler.Sleep(mills);
    }

    private void Exit() {
        scheduler.SwitchProcess(true);
    }

    private int GetPid() {
        return scheduler.currentlyRunning.pid;
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

    private void SendMessage(/*KernelMessage km*/) {
    }

    private KernelMessage WaitForMessage() {
        return null;
    }

    private int GetPidByName(String name) {
//        return scheduler.currentlyRunning.getName()
        return 0;
    }

    private void GetMapping(int virtualPage) {
    }

    private int AllocateMemory(int size) {
        return 0; // change this
    }

    private boolean FreeMemory(int pointer, int size) {
        return true;
    }

    private void FreeAllMemory(PCB currentlyRunning) {
    }

}
