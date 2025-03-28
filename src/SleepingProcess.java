
public class SleepingProcess extends UserlandProcess {
    @Override
    public void main() {
        System.out.println("Sleeping for 5 seconds");
        OS.Sleep(5000);
        System.out.println("Sleeping finished, exiting...");
        OS.Exit();
    }
}
