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
import org.springframework.util.StringUtils;

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
            Captura captura = toCaptura(message);

            uploadCapturaUseCase.execute(captura);

            channel.basicAck(deliveryTag, false);
            log.info("Processamento concluído e ack enviado - transação: {}", message.getId());

        } catch (Exception e) {
            log.error("Erro ao processar transação {}: {}", message.getId(), e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private Captura toCaptura(VideoProcessingMessage message) {
        if (StringUtils.hasText(message.getS3Key())) {
            String s3Key = message.getS3Key();

            return Captura.builder()
                    .id(message.getId())
                    .email(message.getEmail())
                    .fileName(extractFileName(s3Key))
                    .s3Key(s3Key)
                    .build();
        }

        if (StringUtils.hasText(message.getVideo())) {
            byte[] videoBytes = decodeVideo(message.getVideo());

            return Captura.builder()
                    .id(message.getId())
                    .email(message.getEmail())
                    .fileName("video_" + message.getId() + ".mp4")
                    .content(videoBytes)
                    .build();
        }

        throw new IllegalArgumentException("Mensagem não possui vídeo inline nem s3Key para processamento");
    }

    private byte[] decodeVideo(String videoDataUri) {
        String base64Data = videoDataUri;
        int commaIndex = videoDataUri.indexOf(',');
        if (commaIndex >= 0) {
            base64Data = videoDataUri.substring(commaIndex + 1);
        }
        return Base64.getDecoder().decode(base64Data);
    }

    private String extractFileName(String s3Key) {
        int lastSlash = s3Key.lastIndexOf('/');
        return lastSlash >= 0 ? s3Key.substring(lastSlash + 1) : s3Key;
    }
}
