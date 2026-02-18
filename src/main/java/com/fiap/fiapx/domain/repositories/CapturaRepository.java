package com.fiap.fiapx.domain.repositories;

import com.fiap.fiapx.domain.entities.Captura;
import java.io.File;
import java.util.List;

public interface CapturaRepository {
    // Interface para persistência se necessário, ou gateways de infra
    List<File> extractFrames(File videoFile, int intervalSeconds);
    String upload(File file, String path);
    void updateStatus(Integer idTransacao, com.fiap.fiapx.domain.entities.CapturaStatus status);
    void sendErrorEmail(String to, Integer videoId);
}
