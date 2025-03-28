import java.nio.charset.StandardCharsets;

public class FSTestProcess extends UserlandProcess {
    @Override
    public void main() {
        int fd = OS.Open("file myclasses.csv");
        OS.Write(fd, "course,class name,start time,end time\n".getBytes(StandardCharsets.UTF_8));
        OS.Write(fd, "icsi412,operating systems,09:00,10:00\n".getBytes(StandardCharsets.UTF_8));
        OS.Write(fd, "amat220,linear algebra,11:30,12:30\n".getBytes(StandardCharsets.UTF_8));
        OS.Write(fd, "ahis101,us history,13:00,14:00\n".getBytes(StandardCharsets.UTF_8));
        OS.Write(fd, "achm101,chemistry,15:00,16:00\n".getBytes(StandardCharsets.UTF_8));

        byte[] contents = new byte[1024];
        OS.Read(fd, contents.length);
        System.out.println(contents);
        OS.Close(fd);
    }
}
