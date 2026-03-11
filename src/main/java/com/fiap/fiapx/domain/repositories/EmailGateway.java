package com.fiap.fiapx.domain.repositories;

public interface EmailGateway {
    void sendErrorEmail(String to, Integer videoId);
}
