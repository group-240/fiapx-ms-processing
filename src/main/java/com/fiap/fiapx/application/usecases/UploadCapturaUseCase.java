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
            log.info("Iniciando processamento para a transacao: {} ({})", idTransacao, captura.getEmail());
            
            // 1. Notificar Início (PROCESSING)
             repository.updateStatus(idTransacao, CapturaStatus.PROCESSANDO);

            // 2. Salvar vídeo temporariamente
            tempVideoFile = File.createTempFile("video_" + idTransacao + "_", "_" + captura.getFileName());
            try (FileOutputStream fos = new FileOutputStream(tempVideoFile)) {
                fos.write(captura.getContent());
            }

            // 3. Extrair frames a cada x segundos
            List<File> frames = repository.extractFrames(tempVideoFile, secundSplitter);

            // 4. Salvar frames
            for (int i = 0; i < frames.size(); i++) {
                File frame = frames.get(i);
                String path = "user_" + idTransacao + "/" + captura.getFileName() + "/frame_" + (i * 10) + "s.jpg";
                repository.upload(frame, path);
                Files.deleteIfExists(frame.toPath());
            }

            // 5. Notificar Finalização (DONE)
            repository.updateStatus(idTransacao, CapturaStatus.CONCLUIDO);
            log.info("Processamento finalizado com sucesso para o usuário: {}", idTransacao);

        } catch (Exception e) {
            log.error("Erro ao processar vídeo do usuário {}: {}", idTransacao, e.getMessage());
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
