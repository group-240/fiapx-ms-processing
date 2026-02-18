package com.fiap.fiapx.infrastructure.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FIAPX - Video Processor API")
                        .version("1.0.0")
                        .description("API para processamento de vídeos e extração de frames - Tech Challenge")
                        .contact(new Contact()
                                .name("Grupo 240 - FIAPX")
                                .url("https://github.com/group-240/fiapx")));
    }
}
