# FIAPX - Video Processor Service (Tech Challenge)

[![Java Version](https://img.shields.io/badge/Java-17-blue.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.2-brightgreen.svg)](https://spring.io/projects/spring-boot)

Este microsserviço é o motor de processamento de mídia do ecossistema **FIAPX**. Ele foi projetado para extrair frames de vídeos de forma escalável, utilizando processamento assíncrono e armazenamento em nuvem (AWS S3), com notificações de status em tempo real para sistemas externos.

---

## 🏗️ Arquitetura do Sistema

O projeto adota a **Clean Architecture** (Arquitetura Limpa), com a estrutura de pacotes organizada conforme o padrão solicitado:

### Estrutura de Pastas

```text
src/main/java/com/fiap/fiapx/
├── domain/                      # Camada de Domínio (Regras de Negócio)
│   ├── entities/                # Entidades de domínio (Captura, CapturaStatus)
│   ├── repositories/            # Interfaces de repositório e gateways
│   └── exception/               # Exceções customizadas
├── application/                 # Camada de Aplicação (Casos de Uso)
│   ├── usecases/                # Implementação dos Casos de Uso (UploadCapturaUseCase)
│   └── dto/                     # Objetos de Transferência de Dados
├── infrastructure/              # Camada de Infraestrutura (Adaptadores)
│   ├── adapter/                 # Implementações de Repositórios e Clientes
│   └── configuration/           # Configurações do Spring e OpenAPI
└── presentation/                # Camada de Apresentação (Controladores)
    └── controller/              # Endpoints REST
```

---

## 🧩 Explicação das Classes Principais

| Classe | Responsabilidade | Camada |
| :--- | :--- | :--- |
| `VideoController` | Recebe `idTransacao`, `email` e `MultipartFile`. Inicia o processamento assíncrono. | Presentation |
| `UploadCapturaUseCase` | Gerencia o ciclo de vida do processamento (Status: PROCESSING -> DONE/ERROR). | Application |
| `Captura` | Entidade de domínio que representa o vídeo e seus metadados. | Domain |
| `CapturaRepositoryImpl` | Orquestra o uso de processadores de vídeo, storages e notificações. | Infrastructure |
| `FFmpegVideoProcessor` | Utiliza **JavaCV** para extrair frames sem dependências do SO. | Infrastructure |
| `S3StorageAdapter` | Realiza o upload dos frames para o bucket AWS S3. | Infrastructure |
| `EmailNotificationAdapter` | Envia e-mails de alerta em caso de falha no processamento. | Infrastructure |

---

## 🚀 Escalabilidade e Performance

- **Non-blocking I/O**: Uso de `CompletableFuture` para processamento paralelo.
- **Stateless Design**: Permite escalonamento horizontal em clusters.
- **Pure Java**: Independência de binários externos do sistema operacional.

---

## 🧪 Testes e Qualidade

### Cobertura com JaCoCo
Para gerar o relatório de cobertura:
```bash
mvn clean test
```
O relatório estará disponível em: `target/site/jacoco/index.html`

---

## 📖 Guia de Integração (API)

### 1. Upload de Vídeo
Envia um arquivo de vídeo para processamento.

**Endpoint:** `POST /api/capturas/upload`  
**Content-Type:** `multipart/form-data`

**Parâmetros de Query:**
- `idTransacao`: ID da transação (ex: 1)
- `email`: Email para contato (ex: user@fiap.com.br)

**Exemplo cURL:**
```bash
curl -X POST "http://localhost:8080/api/capturas/upload?id=1&email=user@fiap.com.br" \
  -F "file=@video.mp4;type=video/mp4"
```

### 2. Notificação de Status (Saída)
O serviço enviará chamadas **PUT** para o sistema externo configurado:
**Endpoint:** `{notification.endpoint.url}/{idTransacao}`  
**Payload:**
```json
{
  "status": "PROCESSANDO"
}
```

### 3. Notificação por E-mail (Erro)
Em caso de falha, um e-mail será enviado ao usuário:
- **Assunto:** Erro ao processar o video {videoId}
- **Corpo:** Seu video foi interrompido por algum erro

---

## ⚙️ Configuração (application.properties)

```properties
# AWS S3
aws.s3.bucket=fiapx-videos-bucket

# Notificações
notification.endpoint.url=http://api-externa:8888/api/capturas/update-status

# E-mail (SMTP)
spring.mail.host=smtp.exemplo.com
spring.mail.port=587
spring.mail.username=seu-usuario
spring.mail.password=sua-senha
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Storage Local (Validação)
storage.local.path=./capturas-validacao
```

---
**Grupo 240 - FIAPX**  
*Tech Challenge - Fase 4*
