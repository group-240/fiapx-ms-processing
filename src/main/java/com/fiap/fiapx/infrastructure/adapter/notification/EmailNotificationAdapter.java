package com.fiap.fiapx.infrastructure.adapter.notification;

import com.fiap.fiapx.domain.repositories.EmailGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.util.StringUtils;

@Slf4j
@RequiredArgsConstructor
public class EmailNotificationAdapter implements EmailGateway {

    private final JavaMailSender mailSender;

    @Value("${mail.from}")
    private String from;

    @Override
    public void sendErrorEmail(String to, Integer videoId) {
        if (!StringUtils.hasText(to)) {
            log.warn("E-mail de destino não informado. videoId={}", videoId);
            return;
        }

        log.info("Enviando e-mail de erro para: {} sobre o vídeo: {}", to, videoId);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom(from);
            message.setSubject("Erro ao processar o vídeo " + videoId);
            message.setText(
                    String.format(
                            "O processamento do vídeo %s foi interrompido devido a um erro.%n" +
                                    "Por favor, tente novamente mais tarde.",
                            videoId
                    )
            );

            mailSender.send(message);
            log.info("E-mail enviado com sucesso para {}", to);

        } catch (Exception e) {
            // ✅ loga stacktrace completo
            log.error("Falha ao enviar e-mail para {}. videoId={}", to, videoId, e);
        }
    }
}
