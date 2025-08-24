# retail-full-smoketest.ps1
$ErrorActionPreference = "Stop"

function ok($msg){ Write-Host "[OK]  $msg" -ForegroundColor Green }
function ko($msg){ Write-Host "[FAIL] $msg" -ForegroundColor Red }

function Wait-Http($url, $timeoutSec=120){
  $sw = [Diagnostics.Stopwatch]::StartNew()
  while($sw.Elapsed.TotalSeconds -lt $timeoutSec){
    try{
      $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5
      if($r.StatusCode -ge 200 -and $r.StatusCode -lt 500){ return $true }
    } catch { Start-Sleep -Seconds 2 }
  }
  return $false
}

function MustGet {
  param(
    [string]$name,
    [string]$url,
    [string]$expectMatch = $null
  )
  try{
    $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 10
    if($expectMatch){
      if(-not [string]::IsNullOrEmpty($r.Content) -and ($r.Content -match $expectMatch)){
        ok "$($name) -> $($url)"
      } else {
        ko "$($name): resposta nao contem '$expectMatch' -> $($url)"; return $false
      }
    } else {
      ok "$($name) -> $($url)"
    }
    return $true
  } catch {
    ko "$($name): erro -> $($url) ($($_.Exception.Message))"
    return $false
  }
}

# Descobrir prefixos a partir do Gateway (se exposto)
function Get-GatewayPrefixes {
  $map = @{
    "catalog-service"   = @("/catalog","/api/catalog","/services/catalog")
    "orders-service"    = @("/orders","/api/orders","/services/orders")
    "inventory-service" = @("/inventory","/api/inventory","/services/inventory")
    "api-gateway"       = @("")
  }
  try{
    $routes = Invoke-RestMethod "http://localhost:8080/actuator/gateway/routes"
    foreach($r in $routes){
      $id = $r.route_id
      if(-not $id){ $id = $r.id } # versões diferentes
      $paths = @()
      foreach($p in $r.predicates){
        if($p.name -eq "Path"){
          # formatos possíveis
          if($p.args.Path){ $paths += $p.args.Path }
          elseif($p.args.pattern){ $paths += $p.args.pattern }
          elseif($p.args._genkey_0){ $paths += $p.args._genkey_0 }
        }
      }
      foreach($pat in $paths){
        if($pat -match "^/([^/]+)/\*\*"){
          $base = "/$($Matches[1])"
          switch -regex ($id){
            'catalog'   { $map["catalog-service"]   = @($base) }
            'order'     { $map["orders-service"]    = @($base) }
            'inventor'  { $map["inventory-service"] = @($base) }
          }
        }
      }
    }
    ok "Rotas do Gateway inspecionadas"
  } catch {
    ko "Nao consegui ler /actuator/gateway/routes (seguindo com prefixos padrao)"
  }
  return $map
}

# Testar um serviço varrendo prefixos e endpoints de health/metrics
function Test-Service {
  param(
    [string]$svcName,
    [string[]]$prefixes
  )
  $healthOk = $false
  $metricsOk = $false
  $chosenPrefix = $null

  $healthPatterns = @{
    "catalog-service"   = 'ok:catalog-service|"status"\s*:\s*"UP"'
    "orders-service"    = 'ok:orders-service|"status"\s*:\s*"UP"'
    "inventory-service" = 'ok:inventory-service|"status"\s*:\s*"UP"'
  }

  foreach($p in $prefixes){
    # tentar /api/health (custom) e /actuator/health (padrao)
    $u1 = "http://localhost:8080$($p)/api/health"
    $u2 = "http://localhost:8080$($p)/actuator/health"
    if(MustGet "$($svcName) health (api)" $u1 $healthPatterns[$svcName]){ $healthOk = $true; $chosenPrefix = $p; break }
    elseif(MustGet "$($svcName) health (actuator)" $u2 'UP'){ $healthOk = $true; $chosenPrefix = $p; break }
  }

  if($healthOk -and $chosenPrefix){
    $um = "http://localhost:8080$($chosenPrefix)/actuator/prometheus"
    $metricsOk = MustGet "$($svcName) prometheus" $um "jvm_"
  } else {
    ko "$($svcName): nenhum prefixo funcionou"
  }

  return [PSCustomObject]@{
    service = $svcName
    prefix  = $chosenPrefix
    health  = $healthOk
    metrics = $metricsOk
  }
}

# 0) Compose up (idempotente)
if(-not (Test-Path .\deploy\docker-compose.yml)){ ko "deploy/docker-compose.yml nao existe"; exit 1 }
Write-Host "== garantindo docker compose up =="
docker compose -f deploy/docker-compose.yml up -d | Out-Null

# 1) Health basico
Write-Host "`n== aguardando subir =="
if(Wait-Http "http://localhost:8888/actuator/health"){ ok "Config Server UP (8888)" } else { ko "Config Server sem resposta"; exit 1 }
if(Wait-Http "http://localhost:8080/actuator/health"){ ok "API Gateway UP (8080)" } else { ko "Gateway sem resposta"; exit 1 }

# 2) Descobrir rotas e testar serviços via Gateway
Write-Host "`n== health/metrics via Gateway =="
$prefixMap = Get-GatewayPrefixes
$results = @()
$results += Test-Service "catalog-service"   $prefixMap["catalog-service"]
$results += Test-Service "orders-service"    $prefixMap["orders-service"]
$results += Test-Service "inventory-service" $prefixMap["inventory-service"]

# 3) Prometheus (targets)
Write-Host "`n== Prometheus targets =="
try{
  $t = Invoke-RestMethod "http://localhost:9090/api/v1/targets"
  $ups = @($t.data.activeTargets | Where-Object {$_.health -eq "up"}).Count
  if($ups -ge 3){ ok "Prometheus scraping ($ups targets UP)" } else { ko "Prometheus com poucos targets UP ($ups)" }
} catch { ko "Falha consultando Prometheus /api/v1/targets" }

# 4) Grafana
Write-Host "`n== Grafana =="
$null = MustGet "Grafana /api/health" "http://localhost:3000/api/health"

# 5) Tracing: gerar trafego (usa o melhor prefixo encontrado para cada serviço)
Write-Host "`n== gerando trafego para criar spans =="
foreach($r in $results){
  if($r.prefix){
    for($i=0;$i -lt 10;$i++){
      try{ Invoke-WebRequest "http://localhost:8080$($r.prefix)/actuator/health" -UseBasicParsing -TimeoutSec 5 | Out-Null } catch {}
    }
  }
}
Write-Host "== Jaeger services =="
try{
  $svc    = (Invoke-RestMethod "http://localhost:16686/api/services").data
  $found  = @("catalog-service","orders-service","inventory-service","api-gateway") | Where-Object { $svc -contains $_ }
  if($found.Count -ge 1){ ok ("Jaeger recebeu spans (" + ($found -join ", ") + ")") } else { ko "Jaeger ainda sem servicos. Abra http://localhost:16686 para conferir." }
} catch { ko "Falha consultando Jaeger /api/services" }

# 6) Spring Cloud Config: conteudo
Write-Host "`n== Spring Cloud Config (conteudo) =="
$null = MustGet "config catalog-service"   "http://localhost:8888/catalog-service/default"   "propertySources"
$null = MustGet "config orders-service"    "http://localhost:8888/orders-service/default"    "propertySources"
$null = MustGet "config inventory-service" "http://localhost:8888/inventory-service/default" "propertySources"
$null = MustGet "config api-gateway"       "http://localhost:8888/api-gateway/default"       "propertySources"

# 7) Postgres containers
Write-Host "`n== Postgres containers =="
function Sql1($container,$user,$db,$pwd){
  try{
    $cmd = "PGPASSWORD=$pwd psql -U $user -d $db -tAc 'SELECT 1;'"
    $out = docker exec $container bash -lc $cmd 2>$null
    if(($out | Out-String) -match "1"){ ok "$container conectou ($db)" } else { ko "$container nao retornou SELECT 1" }
  } catch { ko "Falha ao conectar no $container ($db)" }
}
Sql1 "retail-microservices-catalog-db-1"   "catalog"   "catalogdb"   "catalog"
Sql1 "retail-microservices-orders-db-1"    "orders"    "ordersdb"    "orders"
Sql1 "retail-microservices-inventory-db-1" "inventory" "inventorydb" "inventory"

# 8) Kafka ping
Write-Host "`n== Kafka ping =="
try{
  docker exec retail-microservices-kafka-1 bash -lc "/opt/bitnami/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic retail-smoketest --partitions 1 --replication-factor 1" | Out-Null
  docker exec retail-microservices-kafka-1 bash -lc "echo ping | /opt/bitnami/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic retail-smoketest" | Out-Null
  $consume = docker exec retail-microservices-kafka-1 bash -lc "/opt/bitnami/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic retail-smoketest --from-beginning --max-messages 1 --timeout-ms 10000"
  if(($consume | Out-String) -match "ping"){ ok "Kafka OK (sent/received 'ping')" } else { ko "Kafka nao consumiu a mensagem" }
} catch { ko "Erro executando Kafka CLI" }

Write-Host "`n== FIM =="
