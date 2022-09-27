package Server.stream;

public interface FrameStream {
    /**
     * returns one frame from the source
     * @param frame the byte array of the frame
     * @return size of the frame
     * @throws Exception
     */
    int getnextframe(byte[] frame) throws Exception;
}
