package com.fiap.fiapx.infrastructure.adapter;

import com.fiap.fiapx.domain.entities.CapturaStatus;
import com.fiap.fiapx.domain.repositories.CapturaRepository;
import com.fiap.fiapx.domain.repositories.EmailGateway;
import com.fiap.fiapx.infrastructure.adapter.notification.HttpNotificationAdapter;
import com.fiap.fiapx.infrastructure.adapter.storage.LocalStorageAdapter;
import com.fiap.fiapx.infrastructure.adapter.storage.S3StorageAdapter;
import com.fiap.fiapx.infrastructure.adapter.video.FFmpegVideoProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CapturaRepositoryImpl implements CapturaRepository {

    private final FFmpegVideoProcessor videoProcessor;
    private final S3StorageAdapter s3Storage;
    private final LocalStorageAdapter localStorage;
    private final HttpNotificationAdapter notificationAdapter;
    private final EmailGateway emailGateway;

    @Value("${storage.local.path:./capturas-validacao}")
    private String localPath;

    @Override
    public List<File> extractFrames(File videoFile, int intervalSeconds) {
        return videoProcessor.extractFrames(videoFile, intervalSeconds);
    }

    @Override
    public String upload(File file, String path) {
        try {
            return s3Storage.upload(file, path);
        } catch (RuntimeException ex) {
            log.warn("S3 indisponível para upload ({}). Usando fallback local para path={}", ex.getMessage(), path);
            String localSaved = localStorage.upload(file, path);
            if (localSaved == null) {
                throw ex;
            }
            return localSaved;
        }
    }

    @Override
    public void updateStatus(Integer idTransacao, CapturaStatus status) {
        notificationAdapter.updateStatus(idTransacao, status);
    }

    @Override
    public void sendErrorEmail(String to, Integer videoId) {
        emailGateway.sendErrorEmail(to, videoId);
    }

    @Override
    public List<String> listFiles(String prefix) {
        List<String> s3Keys = s3Storage.listFiles(prefix);
        if (s3Keys != null && !s3Keys.isEmpty()) {
            return s3Keys;
        }

        try {
            Path dir = Paths.get(localPath, prefix);
            if (!Files.exists(dir)) {
                return List.of();
            }

            List<String> localKeys = new ArrayList<>();
            try (Stream<Path> walk = Files.walk(dir)) {
                walk.filter(Files::isRegularFile)
                        .forEach(p -> {
                            String relative = Paths.get(localPath).relativize(p).toString().replace("\\", "/");
                            localKeys.add(relative);
                        });
            }
            return localKeys;
        } catch (IOException e) {
            log.error("Falha ao listar arquivos no fallback local: {}", e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public byte[] downloadFile(String key) {
        byte[] content = s3Storage.downloadFile(key);
        if (content != null) {
            return content;
        }

        try {
            Path localFile = Paths.get(localPath, key);
            if (Files.exists(localFile)) {
                return Files.readAllBytes(localFile);
            }
            return null;
        } catch (IOException e) {
            log.error("Falha ao baixar arquivo no fallback local: key={}, erro={}", key, e.getMessage(), e);
            return null;
        }
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
