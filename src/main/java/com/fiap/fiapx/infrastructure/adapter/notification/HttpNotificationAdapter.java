package com.fiap.fiapx.infrastructure.adapter.notification;

import com.fiap.fiapx.domain.entities.CapturaStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpNotificationAdapter {

    private final WebClient webClient;

    @Value("${notification.endpoint.url}")
    private String endpointUrl;

    public void updateStatus(Integer idTransacao, CapturaStatus status) {
        String statusValue = status.name();
        log.info("Atualizando status para '{}' para o usuário {}", statusValue, idTransacao);

        Map<String, String> body = Map.of("status", statusValue);

        webClient.put()
                .uri(endpointUrl + "/" + idTransacao)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(e -> log.error("Erro ao atualizar status via PUT: {}", e.getMessage()))
                .subscribe();
    }
}
