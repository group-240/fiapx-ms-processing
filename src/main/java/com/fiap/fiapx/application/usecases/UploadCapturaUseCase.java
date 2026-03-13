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
import java.util.ArrayList;
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
        long processStartTime = System.currentTimeMillis();

        try {
            log.info("===== INICIO PROCESSAMENTO ===== idTransacao={}, email={}, fileName={}", 
                    idTransacao, captura.getEmail(), captura.getFileName());

            long statusStartTime = System.currentTimeMillis();
            repository.updateStatus(idTransacao, CapturaStatus.PROCESSANDO);
            long statusDuration = System.currentTimeMillis() - statusStartTime;
            log.debug("Status atualizado para PROCESSANDO: idTransacao={}, duration={}ms", idTransacao, statusDuration);

            // Obter o arquivo de vídeo: via s3Key (fluxo SQS) ou conteúdo em memória (fluxo direto)
            long downloadStartTime = System.currentTimeMillis();
            if (captura.getS3Key() != null && !captura.getS3Key().isBlank()) {
                log.info("Baixando vídeo do S3: idTransacao={}, s3Key={}", idTransacao, captura.getS3Key());
                tempVideoFile = repository.downloadFromS3(captura.getS3Key());
                long downloadDuration = System.currentTimeMillis() - downloadStartTime;
                log.info("Vídeo baixado do S3 com sucesso: idTransacao={}, tempFile={}, sizeMB={}, duration={}ms", 
                        idTransacao, tempVideoFile.getAbsolutePath(), tempVideoFile.length() / (1024 * 1024), downloadDuration);
            } else {
                log.info("Criando arquivo temporário com conteúdo em memória: idTransacao={}, sizeMB={}", 
                        idTransacao, captura.getContent().length / (1024 * 1024));
                String extension = captura.getFileName() != null && captura.getFileName().contains(".")
                        ? captura.getFileName().substring(captura.getFileName().lastIndexOf("."))
                        : ".mp4";
                tempVideoFile = File.createTempFile("video_" + idTransacao + "_", extension);
                try (FileOutputStream fos = new FileOutputStream(tempVideoFile)) {
                    fos.write(captura.getContent());
                }
                long writeDuration = System.currentTimeMillis() - downloadStartTime;
                log.info("Arquivo temporário criado: idTransacao={}, tempFile={}, sizeMB={}, duration={}ms", 
                        idTransacao, tempVideoFile.getAbsolutePath(), tempVideoFile.length() / (1024 * 1024), writeDuration);
            }

            long extractStartTime = System.currentTimeMillis();
            log.info("Iniciando extração de frames: idTransacao={}, intervaloSegundos={}", idTransacao, secundSplitter);
            List<File> frames = repository.extractFrames(tempVideoFile, secundSplitter);
            long extractDuration = System.currentTimeMillis() - extractStartTime;
            log.info("Frames extraídos com sucesso: idTransacao={}, totalFrames={}, duration={}ms", 
                    idTransacao, frames.size(), extractDuration);

            long uploadStartTime = System.currentTimeMillis();
            log.info("Iniciando upload de {} frames para S3: idTransacao={}", frames.size(), idTransacao);
            int successfulUploads = 0;
            List<String> failedFrames = new ArrayList<>();
            
            for (int i = 0; i < frames.size(); i++) {
                File frame = frames.get(i);
                String path = "transacao_" + idTransacao + "/frame_" + (i * secundSplitter) + "s.jpg";
                
                long frameUploadStart = System.currentTimeMillis();
                try {
                    repository.upload(frame, path);
                    successfulUploads++;
                    long frameUploadDuration = System.currentTimeMillis() - frameUploadStart;
                    log.debug("Frame enviado para S3: idTransacao={}, frameIndex={}, s3Path={}, sizeMB={}, duration={}ms", 
                            idTransacao, i, path, frame.length() / (1024.0 * 1024.0), frameUploadDuration);
                } catch (Exception e) {
                    long frameUploadDuration = System.currentTimeMillis() - frameUploadStart;
                    log.error("Falha no upload do frame para S3: idTransacao={}, frameIndex={}, s3Path={}, duration={}ms, erro={}", 
                            idTransacao, i, path, frameUploadDuration, e.getMessage());
                    failedFrames.add(path);
                }
                
                try {
                    Files.deleteIfExists(frame.toPath());
                    log.debug("Frame temporário deletado: idTransacao={}, file={}", idTransacao, frame.getAbsolutePath());
                } catch (IOException e) {
                    log.warn("Falha ao deletar frame temporário: idTransacao={}, file={}, erro={}", idTransacao, frame.getAbsolutePath(), e.getMessage());
                }
            }
            
            long uploadDuration = System.currentTimeMillis() - uploadStartTime;
            log.info("Upload de frames concluído: idTransacao={}, totalFrames={}, sucessos={}, falhas={}, duration={}ms", 
                    idTransacao, frames.size(), successfulUploads, failedFrames.size(), uploadDuration);
            
            // Validação: não marcar como CONCLUIDO se houver falhas
            if (successfulUploads < frames.size()) {
                String errorMsg = String.format("Falha crítica: apenas %d de %d frames foram enviados para S3 (falhas: %s). Processamento interrompido.",
                        successfulUploads, frames.size(), failedFrames);
                log.error("===== PROCESSAMENTO FALHOU ===== idTransacao={}, {}", idTransacao, errorMsg);
                throw new RuntimeException(errorMsg);
            }

            long finalStatusStartTime = System.currentTimeMillis();
            repository.updateStatus(idTransacao, CapturaStatus.CONCLUIDO);
            long finalStatusDuration = System.currentTimeMillis() - finalStatusStartTime;
            log.debug("Status atualizado para CONCLUIDO: idTransacao={}, duration={}ms", idTransacao, finalStatusDuration);

            long totalProcessDuration = System.currentTimeMillis() - processStartTime;
            log.info("===== PROCESSAMENTO CONCLUIDO ===== idTransacao={}, email={}, totalFrames={}, totalDuration={}ms", 
                    idTransacao, captura.getEmail(), frames.size(), totalProcessDuration);

        } catch (Exception e) {
            long errorDuration = System.currentTimeMillis() - processStartTime;
            log.error("===== ERRO NO PROCESSAMENTO ===== idTransacao={}, email={}, duration={}ms, erro={}", 
                    idTransacao, captura.getEmail(), errorDuration, e.getMessage(), e);
            
            try {
                log.info("Atualizando status para ERRO e enviando notificação por email: idTransacao={}", idTransacao);
                repository.updateStatus(idTransacao, CapturaStatus.ERRO);
                repository.sendErrorEmail(captura.getEmail(), captura.getId());
                log.info("Notificação de erro enviada: idTransacao={}, email={}", idTransacao, captura.getEmail());
            } catch (Exception emailException) {
                log.error("Erro ao enviar email de notificação: idTransacao={}, email={}, erro={}", 
                        idTransacao, captura.getEmail(), emailException.getMessage(), emailException);
            }
        } finally {
            if (tempVideoFile != null) {
                try {
                    log.debug("Deletando arquivo temporário de vídeo: file={}", tempVideoFile.getAbsolutePath());
                    Files.deleteIfExists(tempVideoFile.toPath());
                    log.debug("Arquivo temporário deletado com sucesso");
                } catch (IOException e) {
                    log.warn("Não foi possível deletar arquivo temporário: file={}, erro={}", tempVideoFile.getAbsolutePath(), e.getMessage());
                }
            }
        }
    }
}
