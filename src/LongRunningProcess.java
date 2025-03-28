
public class LongRunningProcess extends UserlandProcess {
    @Override
    public void main() {
        while (true) {
            System.out.println("some long running process...");
            cooperate();
            try{
                Thread.sleep(200);
            } catch (InterruptedException e) {}
        }
    }
}
