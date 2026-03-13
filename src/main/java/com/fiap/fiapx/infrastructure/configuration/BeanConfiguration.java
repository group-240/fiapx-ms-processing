package com.fiap.fiapx.infrastructure.configuration;

import com.fiap.fiapx.domain.repositories.EmailGateway;
import com.fiap.fiapx.infrastructure.adapter.notification.EmailNotificationAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Configuration
public class BeanConfiguration {

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "mail", name = "enabled", havingValue = "true")
    public EmailGateway emailGateway(JavaMailSender mailSender,
                                     @Value("${mail.from:${spring.mail.username:}}") String from) {
        return new EmailNotificationAdapter(mailSender, from);
    }

    @Bean
    @ConditionalOnMissingBean(EmailGateway.class)
    public EmailGateway noOpEmailGateway() {
        return (to, videoId) -> log.info(
                "Envio de e-mail desabilitado. videoId={} destino={}",
                videoId,
                to
        );
    }
}
