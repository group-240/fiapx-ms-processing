package com.fiap.fiapx.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VideoProcessingMessage {
    private Integer id;
    private String email;
    private String video;
}
