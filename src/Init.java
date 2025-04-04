public class Init extends UserlandProcess {
    @Override
    public void main() {
        OS.CreateProcess(new HelloWorld());
        OS.CreateProcess(new GoodbyeWorld());

//        OS.CreateProcess(new LongRunningProcess(), OS.PriorityType.realtime);
//        OS.CreateProcess(new SleepingProcess(), OS.PriorityType.interactive);

//        OS.CreateProcess(new RNGProcess());
//        OS.CreateProcess(new FSTestProcess());

        OS.CreateProcess(new PingProcess());
        OS.CreateProcess(new PongProcess());

        while (true) {
            try { Thread.sleep(50); }
            catch (InterruptedException e) {}
            cooperate();
        }
//        OS.Exit();
    }
}
