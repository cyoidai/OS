public class RNGProcess extends UserlandProcess {
    @Override
    public void main() {
        int random1 = OS.Open("random");
        int random2 = OS.Open("random 123456789");
        int random3 = OS.Open("random 314159265");

        System.out.println(String.format("Today's random numbers are: %d, %d, %d",
                (int)(OS.Read(random1, 1)[0]),
                (int)(OS.Read(random2, 1)[0]),
                (int)(OS.Read(random3, 1)[0])));

        OS.Close(random1);
        OS.Close(random2);
        OS.Close(random3);

        OS.Exit();
    }
}
