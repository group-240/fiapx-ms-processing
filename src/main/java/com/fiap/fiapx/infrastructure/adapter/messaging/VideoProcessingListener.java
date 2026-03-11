package com.fiap.fiapx.infrastructure.adapter.messaging;

import com.fiap.fiapx.application.dto.VideoProcessingMessage;
import com.fiap.fiapx.application.usecases.UploadCapturaUseCase;
import com.fiap.fiapx.domain.entities.Captura;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoProcessingListener {

    private final UploadCapturaUseCase uploadCapturaUseCase;

    @RabbitListener(queues = "${rabbitmq.queue.name}")
    public void onMessage(VideoProcessingMessage message,
                          Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("Mensagem recebida da fila - transação: {}, email: {}",
                message.getId(), message.getEmail());

        try {
            byte[] videoBytes = decodeVideo(message.getVideo());

            Captura captura = Captura.builder()
                    .id(message.getId())
                    .email(message.getEmail())
                    .fileName("video_" + message.getId() + ".mp4")
                    .content(videoBytes)
                    .build();

            uploadCapturaUseCase.execute(captura);

            channel.basicAck(deliveryTag, false);
            log.info("Processamento concluído e ack enviado - transação: {}", message.getId());

        } catch (Exception e) {
            log.error("Erro ao processar transação {}: {}", message.getId(), e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private byte[] decodeVideo(String videoDataUri) {
        String base64Data = videoDataUri;
        int commaIndex = videoDataUri.indexOf(',');
        if (commaIndex >= 0) {
            base64Data = videoDataUri.substring(commaIndex + 1);
        }
        return Base64.getDecoder().decode(base64Data);
    }
}
