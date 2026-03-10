package com.fiap.fiapx.infrastructure.adapter.queue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.fiapx.application.usecases.UploadCapturaUseCase;
import com.fiap.fiapx.domain.entities.Captura;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SqsConsumer {

    private final SqsClient sqsClient;
    private final UploadCapturaUseCase uploadCapturaUseCase;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.queue-url}")
    private String queueUrl;

    @Scheduled(fixedDelay = 10000)
    public void poll() {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(5)
                .waitTimeSeconds(20)
                .build();

        ReceiveMessageResponse response = sqsClient.receiveMessage(request);
        List<Message> messages = response.messages();

        if (messages.isEmpty()) {
            return;
        }

        log.info("Recebidas {} mensagens do SQS", messages.size());

        for (Message message : messages) {
            processMessage(message);
        }
    }

    private void processMessage(Message message) {
        String receiptHandle = message.receiptHandle();
        try {
            JsonNode payload = objectMapper.readTree(message.body());

            Integer id = payload.get("id").asInt();
            String email = payload.get("email").asText();
            String s3Key = payload.get("s3Key").asText();

            log.info("Processando mensagem SQS — id={} s3Key={}", id, s3Key);

            Captura captura = Captura.builder()
                    .id(id)
                    .email(email)
                    .fileName(s3Key.substring(s3Key.lastIndexOf("/") + 1))
                    .s3Key(s3Key)
                    .build();

            uploadCapturaUseCase.execute(captura);

            // Deletar mensagem apenas após processamento bem-sucedido
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(receiptHandle)
                    .build());

            log.info("Mensagem deletada do SQS — id={}", id);

        } catch (Exception e) {
            log.error("Erro ao processar mensagem SQS — body={}: {}", message.body(), e.getMessage());
            // Não deleta: após 3 tentativas vai para DLQ automaticamente
        }
    }
}
