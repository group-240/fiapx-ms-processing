package com.fiap.fiapx.infrastructure.adapter;

import com.fiap.fiapx.domain.entities.CapturaStatus;
import com.fiap.fiapx.domain.repositories.CapturaRepository;
import com.fiap.fiapx.infrastructure.adapter.notification.HttpNotificationAdapter;
import com.fiap.fiapx.infrastructure.adapter.storage.LocalStorageAdapter;
import com.fiap.fiapx.infrastructure.adapter.storage.S3StorageAdapter;
import com.fiap.fiapx.infrastructure.adapter.video.FFmpegVideoProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CapturaRepositoryImpl implements CapturaRepository {

    private final FFmpegVideoProcessor videoProcessor;
    private final S3StorageAdapter s3Storage;
    private final LocalStorageAdapter localStorage;
    private final HttpNotificationAdapter notificationAdapter;

    @Override
    public List<File> extractFrames(File videoFile, int intervalSeconds) {
        return videoProcessor.extractFrames(videoFile, intervalSeconds);
    }

    @Override
    public String upload(File file, String path) {
        return s3Storage.upload(file, path);
    }

    @Override
    public void updateStatus(Integer idTransacao, CapturaStatus status) {
        notificationAdapter.updateStatus(idTransacao, status);
    }

    @Override
    public void sendErrorEmail(String to, Integer videoId) {
        log.warn("Notificação por e-mail desabilitada. videoId={} destino={}", videoId, to);
    }

    @Override
    public List<String> listFiles(String prefix) {
        return s3Storage.listFiles(prefix);
    }

    @Override
    public byte[] downloadFile(String key) {
        return s3Storage.downloadFile(key);
    }

    @Override
    public File downloadFromS3(String s3Key) {
        byte[] content = s3Storage.downloadFile(s3Key);
        try {
            String extension = s3Key.contains(".") ? s3Key.substring(s3Key.lastIndexOf(".")) : ".mp4";
            File tempFile = File.createTempFile("video_download_", extension);
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(content);
            }
            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException("Falha ao salvar vídeo baixado do S3: " + e.getMessage(), e);
        }
    }
}
