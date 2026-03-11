package com.fiap.fiapx.presentation.controller;

import com.fiap.fiapx.application.usecases.DownloadCapturaUseCase;
import com.fiap.fiapx.application.usecases.UploadCapturaUseCase;
import com.fiap.fiapx.domain.entities.Captura;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/capturas")
@RequiredArgsConstructor
@Tag(name = "Capturas", description = "Endpoints para processamento de vídeos e extração de frames")
public class VideoController {

    private final UploadCapturaUseCase uploadCapturaUseCase;
    private final DownloadCapturaUseCase downloadCapturaUseCase;

    @Operation(
        summary = "Upload de um vídeo para processamento",
        description = "Recebe um arquivo de vídeo, userId e email para iniciar o processamento assíncrono."
    )
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadVideo(
            @Parameter(description = "ID da transacao") @RequestParam("id") Integer id,
            @Parameter(description = "Email do usuário") @RequestParam("email") String email,
            @Parameter(description = "Arquivo de vídeo") @RequestParam("file") MultipartFile file) {
        
        log.info("Recebida requisição de upload para a trasanção: {} com o arquivo: {}", id, file.getOriginalFilename());

        try {
            Captura captura = Captura.builder()
                    .id(id)
                    .email(email)
                    .fileName(file.getOriginalFilename())
                    .content(file.getBytes())
                    .build();

            // Processamento assíncrono para escalabilidade
            CompletableFuture.runAsync(() -> uploadCapturaUseCase.execute(captura));
            
            return ResponseEntity.accepted().body("Processamento iniciado para o arquivo: " + file.getOriginalFilename());
            
        } catch (IOException e) {
            log.error("Erro ao ler arquivo {}: {}", file.getOriginalFilename(), e.getMessage());
            return ResponseEntity.internalServerError().body("Erro ao processar o upload do arquivo.");
        }
    }

    @Operation(
            summary = "Download de todos os frames de um usuário em ZIP",
            description = "Busca todos os frames processados do usuário no S3 e retorna um arquivo ZIP."
    )
    @GetMapping(value = "/download/{idTransacao}", produces = "application/zip")
    public ResponseEntity<byte[]> downloadZip(
            @Parameter(description = "ID da transacao") @PathVariable("idTransacao") Integer idTransacao) {

        log.info("Recebida requisição de download ZIP para a transação: {}", idTransacao);

        byte[] zipContent = downloadCapturaUseCase.execute(idTransacao);

        if (zipContent == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"frames_" + idTransacao + ".zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zipContent);
    }
}
