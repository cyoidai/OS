import java.util.Random;

public class RandomDevice implements Device {

    private final Random[] randoms = new Random[10];

    @Override
    public int Open(String s) {
        int index = -1;
        for (int i = 0; i < randoms.length; i++)
            if (randoms[i] == null) {
                index = i;
                break;
            }
        if (index == -1)
            return -1;

        if (s == null || s.isEmpty())
            randoms[index] = new Random();
        else {
            try {
                long seed = Long.parseLong(s);
                randoms[index] = new Random(seed);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return index;
    }

    @Override
    public void Close(int id) {
        randoms[id] = null;
    }

    @Override
    public byte[] Read(int id, int size) {
        byte[] bytes = new byte[size];
        randoms[id].nextBytes(bytes);
        return bytes;
    }

    @Override
    public void Seek(int id, int to) {
        randoms[id].nextBytes(new byte[to]);
    }

    @Override
    public int Write(int id, byte[] data) {
        return 0;
    }
}
