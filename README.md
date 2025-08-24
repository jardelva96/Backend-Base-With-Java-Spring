# Retail Microservices (Spring Boot + Docker + K8s)

Monorepo de referência para varejo:
- Services: catalog, orders, inventory
- Infra: Config Server, API Gateway
- Observabilidade: Prometheus + Grafana (métricas), Jaeger (traces) via OpenTelemetry Collector
- Mensageria: Kafka
- Banco: Postgres (um por serviço)
- Orquestração: Docker Compose (dev), K8s com Kustomize (base + overlay dev)

## Rodar local (dev)
1) `docker compose -f deploy/docker-compose.yml up -d --build`
2) Acessos:
   - Gateway: http://localhost:8080
   - Prometheus: http://localhost:9090
   - Grafana: http://localhost:3000 (admin/admin)
   - Jaeger: http://localhost:16686

## K8s
- Manifests em `k8s/base` e overlay `k8s/overlays/dev`.
- Ajuste imagens e aplique com `kubectl apply -k k8s/overlays/dev`.

## Padrões
- Config central (Spring Cloud Config em modo `native`) lendo `./config`.
- Métricas: `/actuator/prometheus`
- Tracing: OTLP -> OTEL Collector -> Jaeger
