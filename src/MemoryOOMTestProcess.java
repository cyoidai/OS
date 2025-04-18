import java.util.Random;

public class MemoryOOMTestProcess extends UserlandProcess {
    @Override
    public void main() {
        int p = OS.AllocateMemory(1024 * 101);
        if (p == -1)
            throw new RuntimeException("Memory allocation failed, success");
    }
}
