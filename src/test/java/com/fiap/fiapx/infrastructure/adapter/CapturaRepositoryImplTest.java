package com.fiap.fiapx.infrastructure.adapter;

import com.fiap.fiapx.domain.repositories.EmailGateway;
import com.fiap.fiapx.infrastructure.adapter.notification.HttpNotificationAdapter;
import com.fiap.fiapx.infrastructure.adapter.storage.LocalStorageAdapter;
import com.fiap.fiapx.infrastructure.adapter.storage.S3StorageAdapter;
import com.fiap.fiapx.infrastructure.adapter.video.FFmpegVideoProcessor;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CapturaRepositoryImplTest {

    @Test
    void shouldDelegateErrorEmailToEmailGateway() {
        EmailGateway emailGateway = mock(EmailGateway.class);
        CapturaRepositoryImpl repository = new CapturaRepositoryImpl(
                mock(FFmpegVideoProcessor.class),
                mock(S3StorageAdapter.class),
                mock(LocalStorageAdapter.class),
                mock(HttpNotificationAdapter.class),
                emailGateway
        );

        repository.sendErrorEmail("user@fiap.com.br", 42);

        verify(emailGateway).sendErrorEmail("user@fiap.com.br", 42);
    }
}