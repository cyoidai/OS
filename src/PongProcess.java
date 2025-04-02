import java.nio.charset.StandardCharsets;

public class PongProcess extends UserlandProcess {
    @Override
    public void main() {
        System.out.println(String.format("I am pong, pid: %d", OS.GetPID()));
        int pingPID = OS.GetPidByName("PingProcess");
        System.out.println(pingPID);
        KernelMessage send, recv;
        int i;
        while (true) {
            recv = OS.WaitForMessage();
            i = recv.getType() + 1;
            send = new KernelMessage(pingPID, i, String.format("message from pong %d", i).getBytes(StandardCharsets.UTF_8));
            OS.SendMessage(send);
//            OS.Exit();
        }
    }
}
