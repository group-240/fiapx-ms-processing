package com.fiap.fiapx.infrastructure.adapter.queue;

import com.fiap.fiapx.application.usecases.UploadCapturaUseCase;
import com.fiap.fiapx.domain.entities.Captura;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMQConsumer {

    private final UploadCapturaUseCase uploadCapturaUseCase;

    @RabbitListener(queues = "${rabbitmq.queue:video-processing-queue}")
    public void consume(Map<String, Object> payload) {
        Integer id = ((Number) payload.get("id")).intValue();
        String email = (String) payload.get("email");
        String s3Key = (String) payload.get("s3Key");

        log.info("Mensagem recebida do RabbitMQ — id={} s3Key={}", id, s3Key);

        Captura captura = Captura.builder()
                .id(id)
                .email(email)
                .fileName(s3Key.substring(s3Key.lastIndexOf("/") + 1))
                .s3Key(s3Key)
                .build();

        uploadCapturaUseCase.execute(captura);
        log.info("Processamento concluído para id={}", id);
    }
}
