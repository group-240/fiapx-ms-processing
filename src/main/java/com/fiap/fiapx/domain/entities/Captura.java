package com.fiap.fiapx.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Captura {
    private Integer id;
    private String email;
    private String fileName;
    private byte[] content;
    private String s3Key;
    private CapturaStatus status;
}
