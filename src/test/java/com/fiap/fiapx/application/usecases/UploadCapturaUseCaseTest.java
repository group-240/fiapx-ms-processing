package com.fiap.fiapx.application.usecases;

import com.fiap.fiapx.domain.entities.Captura;
import com.fiap.fiapx.domain.entities.CapturaStatus;
import com.fiap.fiapx.domain.repositories.CapturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadCapturaUseCaseTest {

    @Mock
    private CapturaRepository repository;

    private UploadCapturaUseCase uploadCapturaUseCase;

    @BeforeEach
    void setUp() {
        uploadCapturaUseCase = new UploadCapturaUseCase(repository);
    }

    @Test
    void shouldProcessCapturaSuccessfully() throws Exception {
        // Given
        Captura captura = Captura.builder()
                .id(1234)
                .email("user@fiap.com.br")
                .fileName("test.mp4")
                .content(new byte[]{1, 2, 3})
                .build();
        
        File mockFrame = File.createTempFile("frame", ".jpg");
    }

    @Test
    void shouldHandleErrorDuringProcessing() {
        // Given
        Captura captura = Captura.builder()
                .id(1234)
                .email("user@fiap.com.br")
                .fileName("test.mp4")
                .content(new byte[]{1, 2, 3})
                .build();
        
    }
}
