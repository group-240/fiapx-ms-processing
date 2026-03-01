package com.fiap.fiapx.application.usecases;

import com.fiap.fiapx.domain.repositories.CapturaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadCapturaUseCase {

    private final CapturaRepository repository;

        public byte[] execute(Integer idTransacao) {
        log.info("Iniciando geração de ZIP para o usuário: {}", idTransacao);

        // O prefixo no S3 é organizado por idTransacao/videoId/frame_...
        // Para baixar tudo do usuário, usamos apenas o idTransacao como prefixo
        List<String> fileKeys = repository.listFiles("transacao_"+idTransacao + "/");

        if (fileKeys.isEmpty()) {
            log.warn("Nenhum frame encontrado para o usuário: {}", idTransacao);
            return null;
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (String key : fileKeys) {
                byte[] fileContent = repository.downloadFile(key);
                if (fileContent != null) {
                    // Extrair apenas o nome do arquivo da chave completa do S3
                    String fileName = key.substring(key.lastIndexOf("/") + 1);

                    ZipEntry entry = new ZipEntry(fileName);
                    zos.putNextEntry(entry);
                    zos.write(fileContent);
                    zos.closeEntry();
                }
            }

            zos.finish();
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("Erro ao gerar arquivo ZIP para o usuário {}: {}", idTransacao, e.getMessage());
            return null;
        }
    }
}
