import java.util.Random;

public class MemoryAllocateAndFreeProcess extends UserlandProcess {
    @Override
    public void main() {
        Random random = new Random();

        // create memory allocations of varying size
        int[] ps = new int[3];
        ps[0] = OS.AllocateMemory(1024);
        ps[1] = OS.AllocateMemory(2048);
        ps[2] = OS.AllocateMemory(4096);
        for (int i = 0; i < 3; i++)
            if (ps[i] == -1)
                throw new RuntimeException("Memory allocation failed");

        // generate random data and write it to memory
        byte[] data0 = new byte[1024];
        byte[] data1 = new byte[2048];
        byte[] data2 = new byte[4096];
        random.nextBytes(data0);
        random.nextBytes(data1);
        random.nextBytes(data2);
        for (int i = 0; i < 1024; i++)
            Hardware.Write(ps[0] + i, data0[i]);
        for (int i = 0; i < 2048; i++)
            Hardware.Write(ps[1] + i, data1[i]);
        for (int i = 0; i < 4096; i++)
            Hardware.Write(ps[2] + i, data2[i]);

        // ensure the data in memory matches what was generated above
        for (int i = 0; i < 1024; i++) {
            byte data = Hardware.Read(ps[0] + i);
            if (data != data0[i])
                throw new RuntimeException("Memory doesn't match the generated data");
        }
        for (int i = 0; i < 2048; i++) {
            byte data = Hardware.Read(ps[1] + i);
            if (data != data1[i])
                throw new RuntimeException("Memory doesn't match the generated data");
        }
        for (int i = 0; i < 4096; i++) {
            byte data = Hardware.Read(ps[2] + i);
            if (data != data2[i])
                throw new RuntimeException("Memory doesn't match the generated data");
        }

        OS.FreeMemory(ps[0], 1024);
        OS.FreeMemory(ps[1], 2048);
        OS.FreeMemory(ps[2], 4096);
        System.out.println("Memory successfully allocated, checked, and freed!");
    }
}
