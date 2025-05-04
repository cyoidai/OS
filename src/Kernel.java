import java.util.Random;

public class Kernel extends Process implements Device {

    private final Scheduler scheduler;
    /** Array where indices are page address, and values indicate whether that
     * page is in use {@code true} or not in use {@code false}. */
    private final boolean[] usedPages = new boolean[Hardware.MEM_SIZE / Hardware.PAGE_SIZE];
    private final VirtualFileSystem vfs = new VirtualFileSystem();
    private final int pageFile = vfs.Open("file pagefile");
    /** Offset into the page file where the next page should be written to */
    private int pageFileOffset = 0;

    public Kernel(UserlandProcess init) {
        super();
        Hardware.clearTLB();
        scheduler = new Scheduler(init);
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
                case CreateProcess -> OS.retVal = CreateProcess((UserlandProcess) OS.parameters.get(0), (OS.PriorityType) OS.parameters.get(1));
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
        for (int id : scheduler.currentlyRunning.descriptors)
            if (id != -1)
                vfs.Close(id);
        FreeAllMemory();
        scheduler.SwitchProcess(true);
    }

    private int GetPid() {
        return scheduler.currentlyRunning.pid;
    }

    private int GetPidByName(String name) {
        return scheduler.GetPIDByName(name);
    }

    // Devices

    public int Open(String s) {
        PCB p = scheduler.currentlyRunning;
        for (int i = 0; i < p.descriptors.length; i++)
            if (p.descriptors[i] == -1) {
                p.descriptors[i] = vfs.Open(s);
                return i;
            }
        return -1;
    }

    public void Close(int id) {
        PCB p = scheduler.currentlyRunning;
        int vfsID = p.descriptors[id];
        if (vfsID == -1)
            return;
        vfs.Close(vfsID);
        p.descriptors[id] = -1;
    }

    public byte[] Read(int id, int size) {
        PCB p = scheduler.currentlyRunning;
        int vfsID = p.descriptors[id];
        return vfs.Read(vfsID, size);
    }

    public void Seek(int id, int to) {
        PCB p = scheduler.currentlyRunning;
        int vfsID = p.descriptors[id];
        vfs.Seek(vfsID, to);
    }

    public int Write(int id, byte[] data) {
        PCB p = scheduler.currentlyRunning;
        int vfsID = p.descriptors[id];
        return vfs.Write(vfsID, data);
    }

    // ipc

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

    // Memory

    /**
     * Given a virtual page address, get the mapping for that address from the
     * currently running process, and update the TLB with this new mapping.
     * @param virtualPage Virtual page address to lookup.
     */
    private void GetMapping(int virtualPage) {
        Page map = scheduler.currentlyRunning.getMapping(virtualPage);
        if (map == null)
            // no mapping exists, segfault
            return;
        if (map.physicalPage == -1)
            // page is on disk, swap it into memory
            SwapPageIn(map);
        Hardware.updateTLB(virtualPage, map.physicalPage);
    }

    /**
     * Creates a new memory allocation and maps it to the currently running
     * process. -1 on failure.
     * @param size Size to allocate in words.
     * @return The virtual address of the first word in the allocation.
     */
    private int AllocateMemory(int size) {
        if (size % Hardware.PAGE_SIZE != 0)
            return -1;
        int pages = size / Hardware.PAGE_SIZE;
        Page[] allocation = new Page[pages];
        GetFreePages(allocation);
        // update the process' page table
        int virtualAddress = scheduler.currentlyRunning.allocateMemory(allocation);
        if (virtualAddress == -1)
            // process' page table is full
            return -1;
        // mark new pages as "in use"
        for (int i = 0; i < allocation.length; i++)
            usedPages[allocation[i].physicalPage] = true;
        return virtualAddress;
    }

    private boolean FreeMemory(int pointer, int size) {
        if (size % Hardware.PAGE_SIZE != 0 || pointer % Hardware.PAGE_SIZE != 0)
            return false;
        // clear process' page table
        int virtualPage = pointer / Hardware.PAGE_SIZE;
        int pages = size / Hardware.PAGE_SIZE;
        Page[] physicalPages = scheduler.currentlyRunning.freeMemory(virtualPage, pages);
        // mark freed pages "free" in the os
        for (Page page : physicalPages)
            if (page.physicalPage > 0)
                usedPages[page.physicalPage] = false;
        return true;
    }

    private void FreeAllMemory() {
        PCB p = scheduler.currentlyRunning;
        Page[] freedPages = p.freeAllMemory();
        Page page;
        for (int i = 0; i < freedPages.length && freedPages[i] != null; i++) {
            page = freedPages[i];
            if (page.physicalPage > 0)
                usedPages[page.physicalPage] = false;
        }
    }

    /**
     * Searches the kernel's page allocation array for free pages. When free
     * pages are found, they are added to {@code allocationMap} at index
     * {@code last}. When no free pages remain, we borrow from other processes,
     * swapping their page out to disk.
     * @param allocation Array containing pages for the new memory allocation.
     *                   Its length is the number of pages to allocate.
     */
    private void GetFreePages(Page[] allocation) {
        int next = 0;
        // look for free pages
        for (int i = 0; i < usedPages.length && next < allocation.length; i++)
            if (!usedPages[i]) {
                Page p = new Page();
                p.physicalPage = i;
                allocation[next++] = p;
            }
        // borrow pages if necessary
        int start = next;
        while (next < allocation.length) {
            Page victim = FindPageToSwapOut();
            // make sure we don't swap the same page twice
            boolean pageExists = false;
            for (int i = start; i < next; i++)
                if (victim == allocation[i]) {
                    pageExists = true;
                    break;
                }
            if (pageExists)
                continue;
            int physicalAddress = SwapPageOut(victim);
            Page p = new Page();
            p.physicalPage = physicalAddress;
            allocation[next++] = p;
        }
    }

    /**
     * Takes a page currently stored on disk and swaps it into memory. If memory
     * is full, then borrow a page from another, or potentially, our own
     * process. That is, writing the victim's page contents to disk in order
     * to make room for the page we want to swap in.
     * @param page Page currently on disk.
     */
    private void SwapPageIn(Page page) {
        // find a free page to store the page
        int freePage = -1;
        for (int i = 0; i < usedPages.length; i++)
            if (!usedPages[i]) {
                freePage = i;
                break;
            }
        // if no free pages exist, borrow from another process
        if (freePage == -1) {
            Page victim;
            do {
                victim = FindPageToSwapOut();
            } while (victim == page);
            freePage = SwapPageOut(victim);
        }
        // read page contents from disk and write to memory
        int physicalAddress = freePage * Hardware.PAGE_SIZE;
        vfs.Seek(pageFile, page.diskPage * Hardware.PAGE_SIZE);
        byte[] data = vfs.Read(pageFile, Hardware.PAGE_SIZE);
        for (int i = 0; i < data.length; i++)
            Hardware.directWrite(physicalAddress + i, data[i]);
        page.diskPage = -1;
        page.physicalPage = freePage;
        usedPages[freePage] = true;
    }

    /**
     * Takes a page currently stored in memory and swaps it out to disk.
     * @param page A page currently in memory.
     * @return The physical page address that was freed.
     */
    private int SwapPageOut(Page page) {
        byte[] toWrite = new byte[Hardware.PAGE_SIZE];
        int physicalAddress = page.physicalPage * Hardware.PAGE_SIZE;
        for (int i = 0; i < toWrite.length; i++)
            toWrite[i] = Hardware.directRead(physicalAddress + i);
        vfs.Seek(pageFile, pageFileOffset);
        vfs.Write(pageFile, toWrite);
        int freePage = page.physicalPage;
        page.diskPage = pageFileOffset / Hardware.PAGE_SIZE;
        page.physicalPage = -1;
        pageFileOffset += Hardware.PAGE_SIZE;
        usedPages[freePage] = false;
        return freePage;
    }

    private final Random random = new Random();
    /**
     * Picks a random page currently in memory.
     * @return The page.
     */
    private Page FindPageToSwapOut() {
        PCB victim;
        while (true) {
            victim = scheduler.GetRandomProcess();
            Page[] pageTable = victim.getPageTable();
            Page page = pageTable[random.nextInt(pageTable.length)];
            if (page == null || page.physicalPage == -1)
                continue;
            return page;
        }
    }

    /**
     * If the kernel crashes, don't call {@code OS.Exit()}, instead kill the
     * entire machine.
     */
    @Override
    public void onCrash() {
        System.exit(1);
    }
}
