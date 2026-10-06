# Instruções de Implementação: Observabilidade e Métricas (Prometheus + Grafana)

## 1. Contexto
Além da arquitetura distribuída (Manager/Workers) baseada em Spring Batch e Kafka, o projeto exige uma stack de observabilidade para provar a eficiência da solução. Precisamos gerar métricas de desempenho para alimentar um dashboard no Grafana, permitindo a extração de gráficos (throughput, memória, CPU) para o TCC.

## 2. Dependências (Maven/Gradle)
O LLM deve adicionar as seguintes bibliotecas no `pom.xml` ou `build.gradle` dos microsserviços (Manager e Workers):
- `spring-boot-starter-actuator`: Para expor as métricas internas da aplicação.
- `micrometer-registry-prometheus`: Para formatar as métricas no padrão que o banco de dados Prometheus consegue raspar (scrape).
- `micrometer-core`: Para criação de métricas customizadas de negócio.

## 3. Configuração da Aplicação (`application.yml`)
O LLM deve configurar a exposição dos endpoints do Actuator para permitir a coleta do Prometheus. Exemplo obrigatório de configuração:
`yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus, metrics
  endpoint:
    prometheus:
      enabled: true
  metrics:
    tags:
      application: ${spring.application.name} # Para diferenciar o Manager do Worker no Grafana`

## 4. Instrumentação de Código (Métricas Customizadas no Worker)
O código gerado para o Worker Nodes (especificamente no ItemProcessor ou ItemWriter) deve injetar o MeterRegistry do Micrometer para criar métricas de negócio.
O LLM deve implementar:

- Counter (pix.consolidadas.total): Para contar quantas transações PIX foram processadas com sucesso.

- Timer (pix.processamento.tempo): Para medir o tempo exato que o ItemProcessor leva para processar um chunk e consultar a API externa de isenção.

## 5. Infraestrutura Local (Docker Compose)
O LLM deve gerar ou atualizar o arquivo docker-compose.yml na raiz do projeto contendo os seguintes serviços:

` Prometheus:
    Imagem: prom/prometheus:latest
    Porta: 9090:9090
    Volume: Mapear um arquivo prometheus.yml (que o LLM também deve gerar) configurado para fazer o scrape do Manager (ex: localhost:8080/actuator/prometheus) e dos Workers (ex: localhost:8081/actuator/prometheus). `

` Grafana:
    Imagem: grafana/grafana:latest
    Porta: 3000:3000
    Dependência: depends_on: prometheus `

## 6. Dashboards Recomendados (PromQL)
Ao final da implementação, o LLM deve sugerir um arquivo JSON de configuração de Dashboard do Grafana ou listar as queries em PromQL para os seguintes gráficos:

JVM Memory Used: Para comparar o uso de memória do Manager vs Workers.
(Ex: sum(jvm_memory_used_bytes{area="heap", application="worker-node"}))

CPU Usage: Para monitorar o consumo das instâncias durante o Batch.

Kafka Consumer Lag: Para mostrar a fila de partições diminuindo no tópico batch-requests.

Throughput de Transações PIX (Tps): Taxa de PIX processados por segundo.
(Ex: rate(pix_consolidadas_total[1m]))