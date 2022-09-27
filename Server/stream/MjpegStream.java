package Server.stream;//Server.stream.VideoStream

import java.io.FileInputStream;

/**
 * streams a video file frame by frame, only works with Mjpeg files though :/
 */
public class MjpegStream implements FrameStream{

    private FileInputStream fis;

    public MjpegStream(String filename) throws Exception{

        fis = new FileInputStream(filename);
    }

    @Override
    public int getnextframe(byte[] frame) throws Exception
    {
        int length ;
        String length_string;
        byte[] frame_length = new byte[5];

        fis.read(frame_length,0,5);

        length_string = new String(frame_length);
        length = Integer.parseInt(length_string);

        return(fis.read(frame,0,length));
    }
}