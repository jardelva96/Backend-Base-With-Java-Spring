# Backend Base com Spring Boot (branch `dev`)

Projeto inicial pronto para clonar e rodar: microservices com Spring Boot 3 / Java 21, banco PostgreSQL, mensageria Kafka e stack de observabilidade (Prometheus, Grafana, Jaeger e OTel), tudo orquestrado por Docker Compose. Ideal para quem quer começar um backend sem ter que instalar nada localmente além do Docker.

---

## 🧰 O que vem pronto

- **Infra**: Zookeeper + Kafka, três bancos PostgreSQL (catálogo, pedidos, estoque).
- **Serviços**: Config Server (modo `native`), API Gateway, Catalog / Orders / Inventory.
- **Observabilidade**: Prometheus, Grafana, Jaeger e OpenTelemetry Collector.
- **Deploy rápido**: Compose em `deploy/` com build das imagens e bind dos YAMLs do Config Server.

---

## 🗂 Estrutura de pastas

```
deploy/                 # docker-compose e .env para configuração de volumes
config/                 # YAMLs consumidos pelo Config Server (catálogo, pedidos, gateway...)
services/               # código dos serviços Spring Boot (cada um com seu Dockerfile)
prometheus/             # scrape config
grafana/                # datasource provisionado
otel/                   # configuração do collector
README.md
```

---

## ✅ Pré-requisitos

- Docker + Docker Compose.
- (Opcional) Java 21 e Maven apenas se quiser compilar fora do Docker.

---

## 🚀 Comece em minutos

1) **Clone** e troque para a branch `dev`:

```bash
git clone <url-do-repo>
cd Backend-Base-With-Java-Spring
git checkout dev
```

2) **Configure o volume do Config Server** copiando o exemplo de `.env` (fica em `deploy/`):

```bash
cp deploy/.env.example deploy/.env
```

- `CONFIG_DIR` aponta para a pasta `config/`. Em Windows use um caminho **absoluto** (ex.: `C:/Users/.../config`) para evitar problemas de bind de volume.

3) **Suba tudo** (build das imagens + serviços):

```bash
docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d --build
```

4) **Veja os logs** (todos ou apenas de um serviço):

```bash
docker compose -f deploy/docker-compose.yml logs -f
docker compose -f deploy/docker-compose.yml logs -f api-gateway
```

5) **Derrube e limpe** quando terminar:

```bash
docker compose -f deploy/docker-compose.yml down           # mantém volumes
docker compose -f deploy/docker-compose.yml down -v        # remove tudo (inclui dados dos DBs)
```

---

## 🌐 Portas principais

- API Gateway: http://localhost:8080  
- Config Server: http://localhost:8888  
- Kafka: 9092 (interno)  
- Postgres: 5433 (catalog), 5434 (orders), 5435 (inventory)  
- Prometheus: http://localhost:9090  
- Grafana: http://localhost:3000 (admin/admin)  
- Jaeger UI: http://localhost:16686

---

## 🔧 Configuração externa (Config Server)

- Os YAMLs de cada serviço ficam em `./config`.
- O compose monta essa pasta em `/config` dentro do container `config-server`.
- O caminho do host vem de `CONFIG_DIR` definido em `deploy/.env` (pode ser relativo ou absoluto).

---

## 🧪 Verificações rápidas

Use `curl` (Linux/macOS) ou `Invoke-WebRequest` (PowerShell) após subir os containers:

```bash
# Health do Config Server
curl -fsS http://localhost:8888/actuator/health

# YAMLs servidos pelo Config Server
curl -fsS http://localhost:8888/api-gateway/default
curl -fsS http://localhost:8888/catalog-service/default

# Health do gateway e dos serviços via gateway
curl -fsS http://localhost:8080/actuator/health
curl -fsS http://localhost:8080/catalog/actuator/health
curl -fsS http://localhost:8080/orders/actuator/health
curl -fsS http://localhost:8080/inventory/actuator/health
```

---

## 🔁 Ciclo de desenvolvimento

- Altere o código de um serviço em `services/<nome-do-servico>/`.
- Reconstrua só ele quando precisar:

```bash
docker compose -f deploy/docker-compose.yml build catalog-service
docker compose -f deploy/docker-compose.yml up -d catalog-service
```

- Logs individuais ajudam a depurar durante o desenvolvimento:

```bash
docker compose -f deploy/docker-compose.yml logs -f orders-service
```

---

## 🧯 Dicas e problemas comuns

1) **Config Server não enxerga os YAMLs**: confirme se `CONFIG_DIR` no `deploy/.env` aponta para a pasta `config/`. Em Windows use caminho absoluto.
2) **Portas ocupadas**: pare o processo que está na porta (ex.: `netstat -ano | findstr :8888` no Windows) ou troque a porta no compose.
3) **Reset de bancos**: `docker compose -f deploy/docker-compose.yml down -v` para remover volumes.

---

## 🧭 Próximos passos (ideias)

- Seeds/Flyway para popular dados de exemplo.
- Testcontainers para testes de integração.
- Dashboards prontos no Grafana.
- Instrumentação automática com o Java Agent do OTel.
