public class MemoryIllegalAccessTestProcess extends UserlandProcess {
    @Override
    public void main() {
        int p = OS.AllocateMemory(1024);
        Hardware.Read(p + 1024);
    }
}
