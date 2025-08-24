# Backend-Base-With-Java-Spring

Stack de **microservices** com Spring Boot 3 / Java 21 e observabilidade completa. Inclui:
- **Config Server** (modo `native`, lendo arquivos em `./config`)
- **API Gateway** (Spring Cloud Gateway)
- **Catalog / Orders / Inventory** (PostgreSQL + JPA/Hibernate)
- **Kafka + Zookeeper**
- **Prometheus**, **Grafana**, **Jaeger** e **OpenTelemetry Collector**

> Projeto pensado para subir tudo via Docker Compose, com foco em ambiente Windows/PowerShell, mas funciona em Linux/macOS trocando os comandos de PowerShell por `curl` e afins.

---

## 🔎 Estrutura do repositório

    deploy/
      docker-compose.yml
      docker-compose.override.yml        (opcional)
    config/
      api-gateway.yml
      catalog-service.yml
      orders-service.yml
      inventory-service.yml
    services/
      config-server/                     (Dockerfile + app Spring Boot)
      api-gateway/                       (Dockerfile + app)
      catalog-service/                   (Dockerfile + app)
      orders-service/                    (Dockerfile + app)
      inventory-service/                 (Dockerfile + app)
    prometheus/
      prometheus.yml
    grafana/
      provisioning/
        datasources/
          datasource.yml
    otel/
      otel-collector-config.yml
    README.md

---

## 🧰 Requisitos

- Docker + Docker Compose
- Java 21 e Maven **(apenas** se for compilar fora do Docker)
- PowerShell (Windows) para executar os exemplos com `iwr` (Invoke-WebRequest)

---

## 🚀 Subir tudo (build + run)

1) Abrir um terminal **na raiz** do projeto (“SPRING microservices”).

2) Se for a primeira vez, faça o build e suba tudo:

    docker compose -f .\deploy\docker-compose.yml up -d --build

3) Ver logs de todos os serviços:

    docker compose -f .\deploy\docker-compose.yml logs -f

4) Parar e remover:

    docker compose -f .\deploy\docker-compose.yml down

> Dica: quando alterar código Java, você pode `buildar` só o serviço alterado:
>
>     docker compose -f .\deploy\docker-compose.yml build catalog-service
>     docker compose -f .\deploy\docker-compose.yml up -d catalog-service

---

## 🌐 Portas e serviços

- **API Gateway**: http://localhost:8080  
- **Config Server**: http://localhost:8888  
- **Kafka**: 9092 (interno para os serviços)  
- **Zookeeper**: 2181  
- **Postgres** (host → container):
  - catalog-db: **5433 → 5432**
  - orders-db: **5434 → 5432**
  - inventory-db: **5435 → 5432**
- **Prometheus**: http://localhost:9090  
- **Grafana**: http://localhost:3000 (login padrão: `admin` / `admin`)  
- **Jaeger UI**: http://localhost:16686

---

## 🧩 Configurações via Config Server (modo `native`)

Os YAMLs ficam em `./config`. Cada serviço lê suas configs do Config Server:

- `config/api-gateway.yml`
- `config/catalog-service.yml`
- `config/orders-service.yml`
- `config/inventory-service.yml`

O `docker-compose.yml` já monta **o caminho absoluto** do host em `/config` dentro do container **config-server** para evitar problemas no Windows.

---

## ✅ Smoke tests

1) **Config Server**

    iwr http://localhost:8888/actuator/health -UseBasicParsing

    # Deve retornar StatusCode 200.
    # Estes devem trazer JSON com "propertySources" (NÃO 404):
    iwr http://localhost:8888/api-gateway/default -UseBasicParsing | Select -Expand Content
    iwr http://localhost:8888/catalog-service/default -UseBasicParsing | Select -Expand Content
    iwr http://localhost:8888/orders-service/default -UseBasicParsing | Select -Expand Content
    iwr http://localhost:8888/inventory-service/default -UseBasicParsing | Select -Expand Content

2) **API Gateway e serviços**

    # Health do gateway
    iwr http://localhost:8080/actuator/health -UseBasicParsing

    # Rotas registradas
    iwr http://localhost:8080/actuator/gateway/routes -UseBasicParsing | Select -Expand Content

    # Health de cada serviço (via gateway)
    iwr http://localhost:8080/catalog/actuator/health -UseBasicParsing | Select -Expand Content
    iwr http://localhost:8080/orders/actuator/health -UseBasicParsing  | Select -Expand Content
    iwr http://localhost:8080/inventory/actuator/health -UseBasicParsing | Select -Expand Content

> Em Linux/macOS troque `iwr` por `curl`, por exemplo:  
> `curl -fsS http://localhost:8080/actuator/health`

---

## 🧱 Banco de dados (PostgreSQL)

Conexões usadas pelos serviços (dentro da rede do Docker):

- `catalog-service` → `jdbc:postgresql://catalog-db:5432/catalogdb` (user/pwd: `catalog` / `catalog`)
- `orders-service`  → `jdbc:postgresql://orders-db:5432/ordersdb`   (user/pwd: `orders` / `orders`)
- `inventory-service` → `jdbc:postgresql://inventory-db:5432/inventorydb` (user/pwd: `inventory` / `inventory`)

Se quiser acessar via host (ex.: DBeaver), use as portas mapeadas: 5433, 5434 e 5435.

---

## 📈 Observabilidade

- **Prometheus** (scrapes dos serviços): http://localhost:9090  
- **Grafana**: http://localhost:3000 (admin/admin)  
  - DataSource “Prometheus” já provisionado (`http://prometheus:9090`)
- **Jaeger**: http://localhost:16686  
- **OTel Collector** encaminha traces para o Jaeger.

> Para os serviços aparecerem aqui com métricas/trace, precisam expor `actuator/prometheus` (já configurado) e estar instrumentados (Micrometer/OTel). O compose já conecta tudo.

---

## 🧪 Comandos úteis (dev)

- Ver logs de um serviço específico:

      docker compose -f .\deploy\docker-compose.yml logs -f api-gateway

- Reconstruir só um serviço:

      docker compose -f .\deploy\docker-compose.yml build orders-service
      docker compose -f .\deploy\docker-compose.yml up -d orders-service

- Subir apenas os bancos:

      docker compose -f .\deploy\docker-compose.yml up -d catalog-db orders-db inventory-db

- Derrubar **tudo** e volumes (perde dados!):

      docker compose -f .\deploy\docker-compose.yml down -v

---

## 🧯 Troubleshooting

1) **Config Server retorna 404 em `/{app}/default`**

   Causa comum no Windows: o bind `../config:/config` não monta nada no container quando o `docker-compose.yml` está em `deploy/`.  
   Neste projeto, o compose já usa **caminho absoluto**:

       volumes:
         - "C:/Users/Jardel/Desktop/PROJETOS/SPRING microservices/config:/config:ro"

   Se ainda falhar, dentro do container faça:

       docker exec -it deploy-config-server-1 sh -lc "ls -la /config && head -n 20 /config/api-gateway.yml"

2) **Porta já está em uso (e.g., 8888 ou 16686)**

       docker ps --filter "publish=8888" -q | % { docker stop $_; docker rm $_ }
       docker ps --filter "publish=16686" -q | % { docker stop $_; docker rm $_ }

   Ou libere o processo no Windows:

       netstat -ano | findstr :8888
       taskkill /PID <PID> /F

3) **Erro no build do Config Server: “Unable to find a single main class”**

   Isso acontece quando existem **duas classes `main`** no módulo.  
   A classe válida é:

       com.retail.config.ConfigServerApplication  (@EnableConfigServer)

   O `Dockerfile` do config-server já força:

       RUN mvn -q -DskipTests -Dspring-boot.repackage.mainClass=com.retail.config.ConfigServerApplication package

   Garanta que **NÃO** exista uma segunda classe `main` em `com.example.configserver`.

4) **Resetar bancos**

       docker compose -f .\deploy\docker-compose.yml down -v
       docker volume prune    # confirma 'y'

---

## 🔐 Variáveis importantes (compose)

- `SPRING_PROFILES_ACTIVE=native` (config-server)
- `SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCH_LOCATIONS=file:/config` (config-server)
- `SPRING_CONFIG_IMPORT=optional:configserver:http://config-server:8888` (serviços e gateway)
- `SPRING_APPLICATION_NAME` definido para cada serviço no compose/override

---

## 🧭 Roadmap (idéias)

- Seeds/flyway para dados de exemplo
- Testcontainers para testes locais
- Dashboards Grafana prontos
- Instrumentação automática (OTel Java agent) nos serviços






