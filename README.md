# FIAPX Video Processor (`fiapx-ms-processing`)

Microsserviço worker responsável por consumir mensagens de processamento de vídeo, baixar o arquivo do S3, extrair frames com FFmpeg/JavaCV e atualizar o status na API principal (`fiapx`).

---

## Responsabilidade no sistema

- Consumir eventos de processamento no RabbitMQ
- Processar vídeo (extração de frames)
- Armazenar frames no S3
- Notificar status da transação (`PROCESSANDO`, `CONCLUIDO`, `ERRO`)
- Expor health/prometheus para monitoramento

> Em produção no EKS ele funciona como **worker interno** (sem Service público).

---

## Arquitetura (Clean Architecture)

```text
src/main/java/com/fiap/fiapx/
├── domain/            -> Entidades/regras de negócio
├── application/       -> Casos de uso de processamento
├── infrastructure/    -> RabbitMQ, S3, FFmpeg, notificações
├── presentation/      -> Controller (uso local/dev)
└── configuration/     -> Beans/configurações Spring
```

### Componentes chave

- `RabbitMQConsumer`: consome mensagens da fila
- `UploadCapturaUseCase`: orquestra processamento e status
- `FFmpegVideoProcessor`: extrai frames do vídeo
- `S3StorageAdapter`: baixa vídeo e publica frames
- `HttpNotificationAdapter`: chama API principal para atualizar status

---

## Fluxo interno do worker

1. Recebe mensagem com `id`, `userId`, `email`, `s3Key`
2. Atualiza status para `PROCESSANDO`
3. Faz download do vídeo no S3
4. Extrai frames com FFmpeg/JavaCV
5. Faz upload dos frames para o S3
6. Atualiza status para `CONCLUIDO`

Em caso de falha:
- atualiza status para `ERRO`
- registra erro para diagnóstico

---

## Configuração por variáveis de ambiente

| Variável | Uso |
|---|---|
| `S3_BUCKET`, `AWS_REGION` | acesso ao bucket de vídeos/frames |
| `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD` | consumo de fila |
| `RABBITMQ_QUEUE`, `RABBITMQ_EXCHANGE`, `RABBITMQ_ROUTING_KEY` | binding/routing |
| `NOTIFICATION_URL` | endpoint HTTP de update de status |
| `SPLITTER_INTERVAL_SECONDS` | intervalo de extração de frames |

Actuator/Prometheus:
- `/actuator/health`
- `/actuator/prometheus`

---

## Runtime container

O container usa base Debian (`jammy`) e instala libs nativas necessárias para JavaCV/FFmpeg.

Isso resolve o erro clássico:
- `UnsatisfiedLinkError: no jniavutil in java.library.path`

---

## Build e execução

### Local (build)

```bash
mvn clean package
```

### Docker

```bash
docker build -t fiapx-ms-processing:local .
docker run --rm -p 8080:8080 fiapx-ms-processing:local
```

---

## CI/CD

Workflow: `.github/workflows/ci.yml`

- Build Maven (testes skip no pipeline)
- Build/push imagem ECR
- Apply deployment no EKS
- Rollout status

Branches com gatilho automático:
- `fix`
- `main`

