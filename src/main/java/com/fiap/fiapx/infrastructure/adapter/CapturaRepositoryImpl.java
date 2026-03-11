package com.fiap.fiapx.infrastructure.adapter;

import com.fiap.fiapx.domain.entities.CapturaStatus;
import com.fiap.fiapx.domain.repositories.CapturaRepository;
import com.fiap.fiapx.infrastructure.adapter.notification.EmailNotificationAdapter;
import com.fiap.fiapx.infrastructure.adapter.notification.HttpNotificationAdapter;
import com.fiap.fiapx.infrastructure.adapter.storage.LocalStorageAdapter;
import com.fiap.fiapx.infrastructure.adapter.storage.S3StorageAdapter;
import com.fiap.fiapx.infrastructure.adapter.video.FFmpegVideoProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CapturaRepositoryImpl implements CapturaRepository {

    private final FFmpegVideoProcessor videoProcessor;
    private final S3StorageAdapter s3Storage;
    private final LocalStorageAdapter localStorage;
    private final HttpNotificationAdapter notificationAdapter;
    private final EmailNotificationAdapter emailAdapter;

    @Override
    public List<File> extractFrames(File videoFile, int intervalSeconds) {
        return videoProcessor.extractFrames(videoFile, intervalSeconds);
    }

    @Override
    public String upload(File file, String path) {
        // Salva em ambos para validação
        //localStorage.upload(file, path);
        return s3Storage.upload(file, path);
    }

    @Override
    public void updateStatus(Integer idTransacao, CapturaStatus status) {
        notificationAdapter.updateStatus(idTransacao, status);
    }

    @Override
    public void sendErrorEmail(String to, Integer videoId) {
        emailAdapter.sendErrorEmail(to, videoId);
    }

    @Override
    public List<String> listFiles(String prefix) {
        return s3Storage.listFiles(prefix);
    }

    @Override
    public byte[] downloadFile(String key) {
        return s3Storage.downloadFile(key);
    }
}
