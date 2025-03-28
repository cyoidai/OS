
public class VirtualFileSystem implements Device {

    private Device[] devices = new Device[16];
    private int[] ids = {-1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1};

    @Override
    public int Open(String s) {
        int vfsIndex = -1;
        for (int i = 0; i < ids.length; i++)
            if (ids[i] == -1) {
                vfsIndex = i;
                break;
            }
        if (vfsIndex == -1)
            return -1;

        Device device = null;
        int deviceID = -1;

        if (s.startsWith("random")) {
            for (Device dev : devices)
                if (dev instanceof RandomDevice) {
                    device = dev;
                    break;
                }
            if (device == null)
                device = new RandomDevice();
            if (s.length() > 7)
                deviceID = device.Open(s.substring(7));
            else
                deviceID = device.Open(null);
        } else if (s.startsWith("file")) {
            for (Device dev : devices)
                if (dev instanceof FakeFileSystem) {
                    device = dev;
                    break;
                }
            if (device == null)
                device = new FakeFileSystem();
            deviceID = device.Open(s.substring(5));
        }

        if (deviceID != -1) {
            ids[vfsIndex] = deviceID;
            devices[vfsIndex] = device;
            return vfsIndex;
        }
        return -1;
    }

    @Override
    public void Close(int id) {
        if (id < 0 || id >= ids.length)
            return;
        if (devices[id] == null)
            return;
        devices[id].Close(id);
        ids[id] = -1;
    }

    @Override
    public byte[] Read(int id, int size) {
        if (id < 0 || id >= ids.length)
            return null;
        if (devices[id] == null)
            return null;
        return devices[id].Read(id, size);
    }

    @Override
    public void Seek(int id, int to) {
        if (id < 0 || id >= ids.length)
            return;
        if (devices[id] == null)
            return;
        devices[id].Seek(id, to);
    }

    @Override
    public int Write(int id, byte[] data) {
        if (id < 0 || id >= ids.length)
            return -1;
        if (devices[id] == null)
            return -1;
        return devices[id].Write(id, data);
    }
}
