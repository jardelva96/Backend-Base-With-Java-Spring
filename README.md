# Backend Base with Spring Boot (branch `dev`)

Starter-friendly microservices stack that you can clone and run right away: Spring Boot 3 / Java 21 services, PostgreSQL databases, Kafka, and a full observability stack (Prometheus, Grafana, Jaeger, OTel), all orchestrated via Docker Compose. Perfect for bootstrapping a backend without installing anything beyond Docker.

---

## 🧰 What's included

- **Infrastructure**: Zookeeper + Kafka, three PostgreSQL databases (catalog, orders, inventory).
- **Services**: Config Server (native mode), API Gateway, Catalog / Orders / Inventory.
- **Observability**: Prometheus, Grafana, Jaeger, and OpenTelemetry Collector.
- **Fast start**: Compose in `deploy/` builds the images and mounts the Config Server YAMLs.

---

## 🗂 Repository layout

```
deploy/                 # docker-compose and .env for volume configuration
config/                 # YAMLs consumed by the Config Server (gateway, catalog, orders, inventory)
services/               # Spring Boot services (each with its own Dockerfile)
prometheus/             # scrape configuration
grafana/                # provisioned datasource
otel/                   # collector configuration
README.md
```

---

## ✅ Prerequisites

- Docker + Docker Compose
- (Optional) Java 21 and Maven if you want to build outside Docker

---

## 🚀 Get running in minutes

1) **Clone** and switch to the `dev` branch:

```bash
git clone <repository-url>
cd Backend-Base-With-Java-Spring
git checkout dev
```

2) **Configure the Config Server volume** by copying the `.env` example (lives in `deploy/`):

```bash
cp deploy/.env.example deploy/.env
```

- `CONFIG_DIR` should point to the `config/` folder. On Windows, prefer an **absolute path** (e.g., `C:/Users/.../config`) to avoid volume binding issues.

3) **Start everything** (build images + run services):

```bash
docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d --build
```

4) **View logs** (all services or just one):

```bash
docker compose -f deploy/docker-compose.yml logs -f
docker compose -f deploy/docker-compose.yml logs -f api-gateway
```

5) **Tear down** when you're done:

```bash
docker compose -f deploy/docker-compose.yml down           # keep volumes
docker compose -f deploy/docker-compose.yml down -v        # remove everything (including DB data)
```

---

## 🌐 Key ports

- API Gateway: http://localhost:8080  
- Config Server: http://localhost:8888  
- Kafka: 9092 (internal)  
- Postgres: 5433 (catalog), 5434 (orders), 5435 (inventory)  
- Prometheus: http://localhost:9090  
- Grafana: http://localhost:3000 (admin/admin)  
- Jaeger UI: http://localhost:16686

---

## 🔧 External configuration (Config Server)

- Service YAMLs live in `./config`.
- Compose mounts this folder into `/config` inside the `config-server` container.
- The host path comes from `CONFIG_DIR` defined in `deploy/.env` (relative or absolute).

---

## 🧪 Quick checks

Use `curl` (Linux/macOS) or `Invoke-WebRequest` (PowerShell) after the containers are up:

```bash
# Config Server health
curl -fsS http://localhost:8888/actuator/health

# Config Server YAMLs
curl -fsS http://localhost:8888/api-gateway/default
curl -fsS http://localhost:8888/catalog-service/default

# Gateway and downstream health (via gateway)
curl -fsS http://localhost:8080/actuator/health
curl -fsS http://localhost:8080/catalog/actuator/health
curl -fsS http://localhost:8080/orders/actuator/health
curl -fsS http://localhost:8080/inventory/actuator/health
```

---

## 🔁 Development workflow

- Change the code of a service in `services/<service-name>/`.
- Rebuild just that service when needed:

```bash
docker compose -f deploy/docker-compose.yml build catalog-service
docker compose -f deploy/docker-compose.yml up -d catalog-service
```

- Tail logs for a single service while developing:

```bash
docker compose -f deploy/docker-compose.yml logs -f orders-service
```

---

## 🧯 Tips & common issues

1) **Config Server can't see YAMLs**: make sure `CONFIG_DIR` in `deploy/.env` points to `config/`. On Windows, use an absolute path.
2) **Port already in use**: stop the process on that port (e.g., `netstat -ano | findstr :8888` on Windows) or change the port in the compose file.
3) **Reset databases**: run `docker compose -f deploy/docker-compose.yml down -v` to remove volumes.

---

## 🧭 Next ideas

- Seed data / Flyway migrations for sample data.
- Testcontainers for local integration tests.
- Ready-to-use Grafana dashboards.
- Automatic instrumentation with the OTel Java Agent.
