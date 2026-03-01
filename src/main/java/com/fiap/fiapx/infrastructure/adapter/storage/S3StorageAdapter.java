package com.fiap.fiapx.infrastructure.adapter.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

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

    public List<String> listFiles(String prefix) {
        log.info("Listando arquivos no bucket {} com prefixo {}", bucketName, prefix);
        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();
            ListObjectsV2Response response = s3Client.listObjectsV2(listRequest);
            return response.contents().stream()
                    .map(S3Object::key)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erro ao listar arquivos no S3: {}", e.getMessage());
            return List.of();
        }
    }

    public byte[] downloadFile(String key) {
        log.info("Baixando arquivo {} do bucket {}", key, bucketName);
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getRequest);
            return objectBytes.asByteArray();
        } catch (Exception e) {
            log.error("Erro ao baixar arquivo do S3: {}", e.getMessage());
            return null;
        }
    }
}
