package com.fiap.fiapx.infrastructure.adapter.video;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class FFmpegVideoProcessor {

    public List<File> extractFrames(File videoFile, int intervalSeconds) {
        List<File> frames = new ArrayList<>();
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile)) {
            grabber.start();
            double frameRate = grabber.getFrameRate();
            long totalFrames = grabber.getLengthInFrames();
            long durationSeconds = (long) (totalFrames / frameRate);

            Java2DFrameConverter converter = new Java2DFrameConverter();

            for (long time = 0; time <= durationSeconds; time += intervalSeconds) {
                grabber.setTimestamp(time * 1000000L);
                Frame frame = grabber.grabImage();
                if (frame != null) {
                    BufferedImage bi = converter.convert(frame);
                    File frameFile = File.createTempFile("frame_" + time + "s_", ".jpg");
                    ImageIO.write(bi, "jpg", frameFile);
                    frames.add(frameFile);
                    log.info("Frame extraído aos {} segundos", time);
                }
            }
            grabber.stop();
        } catch (Exception e) {
            log.error("Erro ao extrair frames: {}", e.getMessage());
            throw new RuntimeException("Falha no processamento do vídeo", e);
        }
        return frames;
    }
}
