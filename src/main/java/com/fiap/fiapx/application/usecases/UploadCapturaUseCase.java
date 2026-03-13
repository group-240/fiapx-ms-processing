package com.fiap.fiapx.application.usecases;

import com.fiap.fiapx.domain.entities.Captura;
import com.fiap.fiapx.domain.entities.CapturaStatus;
import com.fiap.fiapx.domain.repositories.CapturaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadCapturaUseCase {

    private final CapturaRepository repository;

    @Value("${splitter.interval-seconds}")
    private Integer secundSplitter;

    public void execute(Captura captura) {
        Integer idTransacao = captura.getId();
        File tempVideoFile = null;

        try {
            log.info("Iniciando processamento — transacao={} email={}", idTransacao, captura.getEmail());

            repository.updateStatus(idTransacao, CapturaStatus.PROCESSANDO);

            // Obter o arquivo de vídeo: via s3Key (fluxo SQS) ou conteúdo em memória (fluxo direto)
            if (captura.getS3Key() != null && !captura.getS3Key().isBlank()) {
                log.info("Baixando vídeo do S3 — s3Key={}", captura.getS3Key());
                tempVideoFile = repository.downloadFromS3(captura.getS3Key());
            } else {
                String extension = captura.getFileName() != null && captura.getFileName().contains(".")
                        ? captura.getFileName().substring(captura.getFileName().lastIndexOf("."))
                        : ".mp4";
                tempVideoFile = File.createTempFile("video_" + idTransacao + "_", extension);
                try (FileOutputStream fos = new FileOutputStream(tempVideoFile)) {
                    fos.write(captura.getContent());
                }
            }

            List<File> frames = repository.extractFrames(tempVideoFile, secundSplitter);

            for (int i = 0; i < frames.size(); i++) {
                File frame = frames.get(i);
                String path = "transacao_" + idTransacao + "/frame_" + (i * secundSplitter) + "s.jpg";
                repository.upload(frame, path);
                Files.deleteIfExists(frame.toPath());
            }

            repository.updateStatus(idTransacao, CapturaStatus.CONCLUIDO);
            log.info("Processamento finalizado — transacao={}", idTransacao);

        } catch (Exception e) {
            log.error("Erro ao processar vídeo — transacao={}: {}", idTransacao, e.getMessage());
            repository.updateStatus(idTransacao, CapturaStatus.ERRO);
            repository.sendErrorEmail(captura.getEmail(), captura.getId());
        } finally {
            if (tempVideoFile != null) {
                try {
                    Files.deleteIfExists(tempVideoFile.toPath());
                } catch (IOException e) {
                    log.warn("Não foi possível deletar arquivo temporário: {}", tempVideoFile.getAbsolutePath());
                }
            }
        }
    }
}
