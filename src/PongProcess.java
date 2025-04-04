import java.nio.charset.StandardCharsets;

public class PongProcess extends UserlandProcess {
    @Override
    public void main() {
        int pingPID = OS.GetPidByName("PingProcess");
        if (pingPID == -1)
            throw new RuntimeException("Process 'PingProcess' not found");
        System.out.println(String.format("I am pong (pid: %d) trying to contact 'PingProcess' (pid: %d)", OS.GetPID(), pingPID));
        KernelMessage send, recv;
        int i;
        while (true) {
            recv = OS.WaitForMessage();
            i = recv.getType() + 1;
            send = new KernelMessage(pingPID, i, String.format("message from pong %d", i).getBytes(StandardCharsets.UTF_8));
            OS.SendMessage(send);
        }
    }
}
