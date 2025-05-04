import java.util.Random;

public class Piggy extends UserlandProcess {

    @Override
    public void main() {
        Random random = new Random();
        System.out.println("piggy");

        // allocate memory
        int p = OS.AllocateMemory(100 * 1024);
        if (p == -1)
            throw new RuntimeException("Memory allocation failed");

        // generate random data and write it to memory
        byte[] data = new byte[100 * 1024];
        random.nextBytes(data);
        for (int i = 0; i < 100 * 1024; i++) {
            Hardware.Write(p + i, data[i]);
            cooperate();
        }

        // give enough time for all instances to allocate before checking
        OS.Sleep(2500);

        // ensure the data in memory matches what was generated above
        for (int i = 0; i < 100 * 1024; i++) {
            byte expected = data[i];
            byte actual = Hardware.Read(p + i);
            if (expected != actual)
                throw new RuntimeException("Memory doesn't match the generated data");
            cooperate();
        }

        OS.FreeMemory(p, 100 * 1024);
        System.out.println("Memory successfully allocated, checked, and freed!");
    }
}
