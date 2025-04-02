import java.nio.charset.StandardCharsets;

public class PingProcess extends UserlandProcess {
    @Override
    public void main() {
        System.out.println(String.format("I am ping, pid: %d", OS.GetPID()));
        int pongPID = OS.GetPidByName("PongProcess");
        System.out.println(pongPID);
        KernelMessage send, recv;
        int i = 0;
        while (true) {
            send = new KernelMessage(pongPID, i, String.format("message from ping %d", i).getBytes(StandardCharsets.UTF_8));
            OS.SendMessage(send);
            recv = OS.WaitForMessage();
            i = recv.getType() + 1;
            System.out.println(recv);
//            OS.Exit();
        }
    }
}
