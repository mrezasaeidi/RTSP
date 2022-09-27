package Server.stream;

import Server.ImageTranslator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;

/**
 * it doesn't work
 * @deprecated
 */
public class VideoStream  implements FrameStream {

    private ImageTranslator imageTranslator;
    private static MediaPlayer mediaPlayer;
    private static String source;

    public VideoStream(String source) throws Exception {
        this.source = source;
        imageTranslator = new ImageTranslator(0.5f);
        Media media = new Media("file:///" + source);
        mediaPlayer = new MediaPlayer(media);
    }

    @Override
    public int getnextframe(byte[] frame) throws Exception {
        MediaView mediaView = new MediaView();
        mediaView.setFitWidth(250);
        mediaView.setFitHeight(400);
        mediaView.setMediaPlayer(mediaPlayer);
        WritableImage wim = new WritableImage(250, 400);
        mediaView.snapshot(null, wim);

        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(wim, null);
        byte[] imageBytes = imageTranslator.compress(bufferedImage);
        int length = imageBytes.length;

        System.arraycopy(imageBytes, 0, frame, 0, length);
        return length;
    }

}
