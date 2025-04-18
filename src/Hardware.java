import java.util.Random;

public class Hardware {

    public static final int MEM_SIZE = 1024 * 1024;
    /** Size of each page in bytes */
    public static final int PAGE_SIZE = 1024;
    /** Number of cached entries (rows) the TLB can store */
    private static final int TLB_SIZE = 2;
    private static final byte[] memory = new byte[MEM_SIZE];
    /** Translation lookaside buffer, where the first contains virtual page
     * addresses and the second contains physical page addresses. */
    private static final int[][] tlb = new int[TLB_SIZE][2];

    public static byte Read(int address) {
        return memory[translateAddress(address)];
    }

    public static void Write(int address, byte value) {
        memory[translateAddress(address)] = value;
    }

    /**
     * Translates a virtual address into its corresponding physical address. If
     * no physical address exists, terminate the currently running process.
     * @param virtualAddress Virtual address to lookup.
     * @return Physical address
     */
    private static int translateAddress(int virtualAddress) {
        int virtualPage = virtualAddress / PAGE_SIZE;
        int physicalPage = searchTLB(virtualPage);
        if (physicalPage == -1)
            OS.GetMapping(virtualPage);
        physicalPage = searchTLB(virtualPage);
        if (physicalPage == -1) {
            // no physical address match, segfault
            System.out.println("Segmentation fault: Illegal memory access");
            OS.Exit();
        }
        return (physicalPage * PAGE_SIZE) + (virtualAddress % PAGE_SIZE);
    }

    /**
     * Searches the TLB for a virtual page address that matches
     * {@code virtualPage}, returning its associated physical page address.
     * Returns -1 if no entry exists.
     * @param virtualPage Virtual page address.
     * @return Physical page address associated with {@code virtualPage}.
     */
    private static int searchTLB(int virtualPage) {
        for (int i = 0; i < tlb.length; i++)
            if (tlb[i][0] == virtualPage)
                return tlb[i][1];
        return -1;
    }

    private static final Random rng = new Random();
    /**
     * Overwrites a random row TLB with a new entry.
     * @param virtualPage Virtual page address.
     * @param physicalPage Physical page address.
     */
    public static void updateTLB(int virtualPage, int physicalPage) {
        int row = rng.nextInt(tlb.length);
        tlb[row][0] = virtualPage;
        tlb[row][1] = physicalPage;
    }

    /**
     * Clears the translation lookaside buffer, setting all its values to -1.
     * Also required to initialize the TLB.
     */
    public static void clearTLB() {
        for (int i = 0; i < tlb.length; i++) {
            tlb[i][0] = -1;
            tlb[i][1] = -1;
        }
    }
}
