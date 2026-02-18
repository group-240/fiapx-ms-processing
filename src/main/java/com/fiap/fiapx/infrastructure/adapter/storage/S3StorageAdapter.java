package com.fiap.fiapx.infrastructure.adapter.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3StorageAdapter {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public String upload(File file, String path) {
        log.info("Fazendo upload para o S3: {}/{}", bucketName, path);
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(path)
                    .contentType("image/jpeg")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
            return "s3://" + bucketName + "/" + path;
        } catch (Exception e) {
            log.error("Erro ao fazer upload para o S3: {}", e.getMessage());
            return null;
        }
    }
}
