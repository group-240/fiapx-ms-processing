package com.fiap.fiapx.infrastructure.adapter.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Component
public class LocalStorageAdapter {

    @Value("${storage.local.path:./capturas-validacao}")
    private String localPath;

    public String upload(File file, String path) {
        log.info("Salvando frame localmente em: {}/{}", localPath, path);
        try {
            Path targetPath = Paths.get(localPath, path);
            Files.createDirectories(targetPath.getParent());
            Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return targetPath.toString();
        } catch (IOException e) {
            log.error("Erro ao salvar frame localmente: {}", e.getMessage());
            return null;
        }
    }
}
