import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FakeFileSystem implements Device {

    private final RandomAccessFile[] files = new RandomAccessFile[10];

    @Override
    public int Open(String s) {
        if (s == null || s.isEmpty())
            throw new RuntimeException("Invalid file name");
        for (int i = 0; i < files.length; i++)
            if (files[i] == null) {
                try {
                    files[i] = new RandomAccessFile(s, "rw");
                } catch (FileNotFoundException e) {
                    throw new RuntimeException("File not found");
                }
                return i;
            }
        return -1;
    }

    @Override
    public void Close(int id) {
        try {
            files[id].close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        files[id] = null;
    }

    @Override
    public byte[] Read(int id, int size) {
        byte[] bytes = new byte[size];
        try {
            files[id].read(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    @Override
    public void Seek(int id, int to) {
        try {
            files[id].seek(to);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int Write(int id, byte[] data) {
        try {
            files[id].write(data);
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        return data.length;
    }
}
