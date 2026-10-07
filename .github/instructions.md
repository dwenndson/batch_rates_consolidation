# Escopo principal do projeto: microsserviço distribuído para consolidação de tarifas Pix

## Diretriz de atuação

Aja como um Engenheiro de Software Sênior e Especialista em Arquitetura de
Sistemas Distribuídos, com profundo conhecimento em Java, Spring Boot, Spring
Batch e Apache Kafka.

O objetivo é criar o código-fonte de um microsserviço escalável para
consolidação de tarifas de transações PIX, aplicando o padrão de Particionamento
Remoto (Remote Partitioning) do Spring Batch.

## Regra absoluta de escopo

Foque **exclusivamente** na criação da arquitetura distribuída,
microsserviços e arquitetura orientada a eventos.

**Não crie, não referencie e não gere código para uma versão legada ou
monolítica baseada em agendamento.** Qualquer decisão técnica, documentação,
configuração ou implementação deve respeitar essa restrição.

## 1. Arquitetura geral — Remote Partitioning

O processamento deve ser dividido entre dois perfis lógicos da mesma aplicação,
ativados por Spring Profiles:

- **Manager (Nó Gerenciador):** orquestra o Job e distribui as partições;
- **Workers (Nós Executores):** processam as partições horizontalmente.

O Kafka é o barramento assíncrono de distribuição da carga. O foco é permitir
escalabilidade horizontal real dos Workers, sem concentrar o processamento de
negócio no Manager.

O Manager deve iniciar uma execução exclusivamente por meio de um endpoint REST
protegido. A API deve validar a solicitação, criar os parâmetros do Job e
delegar a orquestração ao Spring Batch. Não utilizar agendamento ou qualquer
outro mecanismo alternativo ao endpoint REST como mecanismo de disparo.

## 2. Componente Manager — Nó Gerenciador

O Manager deve:

- orquestrar a execução do Job;
- acessar o banco de dados;
- identificar as transações PIX pendentes;
- calcular os limites de particionamento, como `minId` e `maxId`;
- dividir lotes grandes em partições, por exemplo, 1 milhão de IDs em partições
  de 10.000 IDs;
- publicar os metadados de execução no tópico Kafka `batch-requests`;
- publicar instruções contendo o identificador da execução, identificador da
  partição, `minId`, `maxId` e o payload Pix completo da partição;
- possuir um listener no tópico `batch-replies`;
- monitorar o status de conclusão dos Workers;
- encerrar o lote com segurança após todas as partições terminarem;
- garantir que o `JobRepository` atualize o estado transacional do Job.

O Manager **não deve processar regras de negócio diretamente**. Ele é
responsável apenas por orquestração, particionamento, comunicação e controle
da execução.

## 3. Componente Worker — Nós Executores

Cada Worker deve:

- consumir instruções do tópico Kafka `batch-requests`;
- receber o range de IDs e o payload Pix completo da partição;
- acionar um `Step` Spring Batch baseado em chunks;
- processar apenas as transações pertencentes à partição recebida;
- publicar obrigatoriamente uma mensagem de conclusão no tópico
  `batch-replies` após concluir a partição;
- informar sucesso, falha, identificador da execução, identificador da
  partição e detalhes necessários para retomada.

### Ciclo Spring Batch do Worker

O Step deve conter:

1. **ItemReader:** ler as transações PIX correspondentes ao range
   `minId`/`maxId`;
2. **ItemProcessor:** aplicar as regras de negócio e realizar uma chamada de
   rede simulada, usando `RestTemplate` ou `FeignClient`, para uma API externa
   que valide ou verifique isenções tarifárias da conta;
3. **ItemWriter:** salvar os dados consolidados e o cálculo da tarifa final em
   uma tabela correspondente à entidade `ConsolidadoPix`.

O Worker deve ser stateless sempre que possível, permitindo múltiplas
instâncias e escalabilidade horizontal por meio dos grupos de consumidores
Kafka.

## 4. Requisitos técnicos e infraestrutura

Utilize a seguinte stack:

- Java 21;
- Spring Boot 4.x, respeitando a versão efetivamente configurada no projeto;
- Spring Batch compatível com a versão do Spring Boot;
- Spring Kafka;
- Spring Data JPA;
- PostgreSQL ou outro banco relacional compatível;
- Apache Kafka.

O `JobRepository` deve ser configurado em um banco relacional compartilhado
entre Manager e Workers para controle transacional do estado do Job,
partições, Steps e retomadas após falhas.

As configurações globais devem contemplar:

- conexão com o banco de dados;
- inicialização do schema do Spring Batch;
- bootstrap servers do Kafka;
- serializers e deserializers;
- grupos de consumidores;
- tópicos `batch-requests` e `batch-replies`;
- política de retry e dead-letter topic;
- confirmação/commit compatível com o processamento do Spring Batch;
- observabilidade sem exposição de dados pessoais ou credenciais.

## 5. Domínio Pix

As entidades principais são:

- **`TransacaoPix`:** representa o Pix bruto, incluindo identificadores como
  `endToEndId`, valor, dados do pagador e conta relacionada;
- **`ConsolidadoPix`:** representa o resultado processado, incluindo a tarifa
  calculada, status e data de consolidação.

O contrato de dados Pix deve usar como referência o Swagger/OpenAPI oficial do
Banco Central:

- <https://bacen.github.io/pix-api/index.html>
- <https://bacen.github.io/pix-api/openapi.yaml>

Use tipos adequados para dinheiro e datas, como `BigDecimal` e
`OffsetDateTime`/`Instant`. Preserve o `endToEndId` como identificador de
idempotência e evite duplicidade quando uma partição for reprocessada.

## 6. Resiliência e consistência

O projeto deve:

- suportar falhas e retomadas de Workers;
- permitir reprocessamento seguro de partições;
- garantir idempotência por `endToEndId` ou chave natural equivalente;
- evitar confirmação prematura de mensagens Kafka;
- tratar mensagens inválidas de forma explícita;
- usar retry e dead-letter topic conforme os padrões do Spring Kafka;
- manter o estado do Job no `JobRepository`;
- não descartar exceções silenciosamente;
- evitar processamento duplicado em `ConsolidadoPix`;
- manter operações de leitura, cálculo e escrita transacionalmente coerentes.

## 7. Instruções de saída e ordem de implementação

Gere o código passo a passo, seguindo esta ordem:

1. dependências no `pom.xml` ou `build.gradle`;
2. configurações globais do Kafka e do banco de dados em `application.yml`;
3. configurações e tabelas do `JobRepository`;
4. entidades, DTOs, repositories e mapeamentos;
5. contratos das mensagens `batch-requests` e `batch-replies`;
6. componentes do Manager;
7. componentes do Worker;
8. configuração do `ItemReader`, `ItemProcessor` e `ItemWriter`;
9. integração simulada com a API externa de isenção;
10. tratamento de retry, DLT, idempotência e retomada;
11. testes unitários e de integração;
12. documentação operacional dos profiles, tópicos e parâmetros.

Mantenha o código limpo, com boas práticas de Clean Code, separação clara de
responsabilidades e comentários apenas nas partes críticas da infraestrutura
do Spring Batch, Kafka e do controle transacional.

## 8. Restrições de implementação

- Não implementar fluxo legado ou monolítico baseado em agendamento.
- Não colocar regras de negócio no Manager.
- Não usar o Kafka apenas como fila genérica sem integração com o
  Remote Partitioning.
- Não utilizar `double` ou `float` para valores monetários.
- Não registrar CPF, CNPJ, nome, chave Pix ou payload completo em logs.
- Não armazenar credenciais, tokens ou segredos no código-fonte.
- Não criar soluções que dependam de uma única instância de Worker.
- Não confirmar uma mensagem antes que o processamento da partição esteja
  persistido com sucesso.
