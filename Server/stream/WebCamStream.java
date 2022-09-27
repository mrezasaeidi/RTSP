package Server.stream;

import Server.ImageTranslator;
import com.github.sarxos.webcam.Webcam;

public class WebCamStream implements FrameStream {

    private Webcam webcam;
    private ImageTranslator imageTranslator;

    public WebCamStream()
    {
        //convert images to less size to be able to go through udp
        imageTranslator = new ImageTranslator(0.3f);
        webcam = Webcam.getDefault();
        webcam.open();
    }

    @Override
    public int getnextframe(byte[] frame) throws Exception {
        byte[] imageBytes = imageTranslator.compress(webcam.getImage());
        int length = imageBytes.length;

        System.arraycopy(imageBytes, 0, frame, 0, length);
        return length;
    }
}
