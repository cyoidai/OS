public class KernelMessage {

    private int senderPID = -1;
    private int targetPID;
    private int type;
    private byte[] data;

    public KernelMessage(int targetPID, int type, byte[] data) {
        this.targetPID = targetPID;
        this.type = type;
        this.data = data;
    }

    /**
     * Instantiates a copy of an existing {@code KernelMessage}
     * @param km
     */
    public KernelMessage(KernelMessage km) {
        this(km.targetPID, km.type, km.data);
    }

    public int getSenderPID() {
        return senderPID;
    }

    /**
     * Sets the sender PID for this message. Should be called by the kernel
     * ONLY. While this can be called from userland, it will always be called
     * again by the kernel.
     * @param pid Message sender's process ID.
     */
    public void setSenderPID(int pid) {
        senderPID = pid;
    }

    public int getTargetPID() {
        return targetPID;
    }

    public int getType() {
        return type;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return String.format("%s senderpid=%d,targetpid=%d,type=%d,msg=%s", super.toString(), senderPID, targetPID, type, new String(data));
    }
}
