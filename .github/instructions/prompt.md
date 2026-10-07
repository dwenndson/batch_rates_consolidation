
# Prompt de implementação: recebimento de informações Pix via Kafka

## Contexto

Este projeto implementa a consolidação de tarifas de transações Pix com Java 21,
Spring Boot 4.x, Spring Batch compatível, Apache Kafka e PostgreSQL.

O Kafka deve ser utilizado exclusivamente no particionamento remoto do Spring
Batch. Portanto, não implemente um consumidor de eventos Pix em tempo real como
substituto do fluxo de `batch-requests` e `batch-replies`. A mensagem Pix
descrita neste documento representa o registro de negócio que será processado
por um worker, depois que o Manager dividir o trabalho em partições.

## Fonte do contrato Pix

Use como referência normativa o Swagger/OpenAPI oficial do Banco Central:

- Interface: <https://bacen.github.io/pix-api/index.html>
- Especificação: <https://bacen.github.io/pix-api/openapi.yaml>
- Versão consultada: `2.10.0`

O contrato do BACEN pode evoluir de forma compatível. A desserialização deve
aceitar campos adicionais e não deve depender da ordem dos campos JSON. Não
invente campos obrigatórios que não estejam no contrato ou que sejam específicos
da implementação local.

## Objetivo

Implementar o suporte para receber e processar uma mensagem Kafka contendo
informações de um ou mais Pix recebidos. O modelo deve permitir:

1. desserializar o JSON de negócio com segurança;
2. preservar os identificadores e valores necessários para a consolidação;
3. validar campos obrigatórios e regras básicas de formato;
4. transformar cada Pix em `TransacaoPix`;
5. calcular a tarifa no `ItemProcessor`;
6. persistir o resultado como `ConsolidadoPix`;
7. reprocessar a partição com segurança, sem gerar cobrança duplicada.

## Separação entre envelope Kafka e payload Pix

Não misture metadados de infraestrutura com o contrato do BACEN. O envelope
abaixo é interno da aplicação e pode ser ajustado conforme a configuração do
Remote Partitioning:

```json
{
  "jobExecutionId": 12345,
  "stepExecutionId": 67890,
  "partitionId": "pix-0001",
  "partitionStartId": 1,
  "partitionEndId": 1000,
  "pix": [
    {
      "endToEndId": "E1234567820200909123456789012345",
      "txid": "7978c0c97ea64255",
      "valor": "100.00",
      "horario": "2020-09-09T20:15:00.358Z",
      "chave": "financeiro@example.com",
      "pagador": {
        "nome": "Nome do Pagador",
        "cpf": "12345678901"
      },
      "infoPagador": "Pagamento referente ao pedido 123",
      "devolucoes": []
    }
  ]
}
```

O campo `pix` deve ser uma coleção mesmo quando a mensagem transportar apenas
um registro. Se a integração receber diretamente o formato de webhook da API
Pix, mantenha o mesmo formato de negócio e apenas adapte o envelope técnico no
adapter Kafka.

## Modelo de negócio esperado

Criar um DTO imutável para o payload Pix, preferencialmente como `record`,
contendo os campos compatíveis com o schema do BACEN:

- `endToEndId`: identificador E2E da transação; obrigatório e único para
  idempotência;
- `txid`: identificador da cobrança, quando existente; pode não existir em
  transações sem cobrança;
- `valor`: valor monetário recebido, representado como `BigDecimal`, nunca como
  `double` ou `float`;
- `horario`: data e hora da liquidação/recebimento, representada como
  `OffsetDateTime` ou `Instant`;
- `chave`: chave Pix do recebedor, quando disponível;
- `pagador`: dados de identificação do pagador conforme o schema Pix, sem
  assumir que CPF e CNPJ estejam simultaneamente preenchidos;
- `infoPagador`: informação livre enviada pelo pagador, opcional;
- `devolucoes`: lista opcional de devoluções, preservando o identificador,
  valor, horário, status e motivo conforme o schema oficial.

Não use `cpfCnpjPagador` como campo obrigatório do DTO externo. Se o domínio
interno precisar dessa conveniência, derive-a em um mapper a partir de `cpf` ou
`cnpj`, mantendo a origem explícita.

## Regras de validação

Aplicar somente validações compatíveis com o contrato e com o domínio:

- rejeitar mensagem sem `pix`, ou com coleção vazia quando ela representar
  trabalho a processar;
- rejeitar item sem `endToEndId`;
- rejeitar `valor` nulo, negativo ou com precisão incompatível com dinheiro;
- rejeitar data inválida;
- validar o formato dos identificadores sem impor tamanho diferente do
  especificado pelo BACEN;
- tratar `txid`, `chave`, `pagador`, `infoPagador` e `devolucoes` como opcionais
  quando o schema permitir;
- rejeitar mensagens malformadas no deserializer e encaminhá-las para o
  mecanismo padrão de erro/DLT do projeto, sem descarte silencioso.

Mensagens inválidas não devem interromper indefinidamente o consumo nem ser
confirmadas como processadas antes do tratamento do erro. Configure retry e
dead-letter topic seguindo os padrões do Spring Kafka usados no projeto.

## Idempotência e persistência

O `endToEndId` deve ser a chave natural da transação Pix. O processamento deve
ser idempotente porque partições podem ser reprocessadas após falha de worker.

- criar índice/restrição única para `endToEndId`;
- não duplicar `ConsolidadoPix` em uma nova tentativa;
- definir explicitamente o comportamento para uma mesma transação recebida com
  dados diferentes;
- persistir a transação original e o consolidado em operações transacionais;
- não calcular tarifa novamente quando o consolidado já estiver concluído,
  salvo quando existir uma política explícita de reprocessamento.

Valores monetários devem ser calculados com `BigDecimal`, usando escala e
arredondamento definidos pela regra de negócio. Nunca utilizar ponto flutuante.

## Fluxo Spring Batch + Kafka

Implementar o fluxo respeitando a arquitetura existente:

1. o Manager consulta no PostgreSQL as transações pendentes;
2. o Manager cria partições por faixa de identificadores;
3. o Manager publica instruções no tópico `batch-requests`;
4. o Worker consome a instrução da partição;
5. o Worker busca ou desserializa os registros Pix associados;
6. o `ItemProcessor` consulta a API externa de isenção;
7. o `ItemProcessor` calcula a tarifa;
8. o `ItemWriter` salva `ConsolidadoPix` de forma idempotente;
9. o Worker publica o resultado no tópico `batch-replies`;
10. o `JobRepository` mantém o estado e permite retomada após falha.

Não criar um tópico de ingestão em tempo real sem uma decisão arquitetural
explícita. Nesta arquitetura, a mensagem de particionamento deve transportar o
payload Pix completo da partição, além de `minId` e `maxId`. Documentar limites
de tamanho, aplicar validação de lote e configurar `max.poll.records`,
`fetch.max.bytes` e limites equivalentes para evitar estouro de memória.

O Manager deve iniciar cada execução exclusivamente por meio de um endpoint REST
protegido. O endpoint deve validar os parâmetros da execução e delegar o
controle ao Spring Batch. Não implementar nem documentar disparos agendados ou
qualquer mecanismo alternativo ao endpoint REST.

## Kafka e serialização

- usar JSON com `Jackson`;
- configurar `JsonDeserializer` com tipos confiáveis e classes explicitamente
  autorizadas;
- definir `group.id`, chave da mensagem e política de commit de forma
  compatível com o Remote Partitioning;
- usar `endToEndId` como chave de negócio quando a mensagem representar um Pix
  individual; para instruções de partição, usar `partitionId`;
- garantir que retry não provoque perda de mensagem nem confirmação prematura;
- incluir observabilidade com `partitionId`, `jobExecutionId`,
  `stepExecutionId` e `endToEndId`, sem registrar dados pessoais ou payload
  completo em logs.

## Privacidade e segurança

CPF, CNPJ, nome, chave Pix e informações livres são dados sensíveis para
observabilidade. Não registrar esses valores em nível `INFO`. Aplicar
mascaramento em logs e nunca armazenar credenciais Kafka ou tokens da API
externa no código-fonte.

## Testes obrigatórios

Criar testes unitários e de integração para:

- desserialização de um Pix válido;
- payload com múltiplos itens em `pix`;
- ausência de `txid` e de dados opcionais;
- presença de CPF ou CNPJ no pagador;
- devolução associada ao Pix;
- valor monetário sem erro de precisão;
- data com offset;
- campos adicionais introduzidos por evolução compatível do schema;
- mensagem inválida encaminhada para erro/DLT;
- reprocessamento do mesmo `endToEndId` sem duplicidade;
- falha do worker com retomada da partição;
- publicação e consumo dos tópicos `batch-requests` e `batch-replies`.

Use os testes existentes e os padrões do projeto. Não alterar o contrato
oficial do BACEN para adequá-lo ao modelo interno; faça a conversão em um
mapper claramente identificado.

## Critérios de aceite

Considerar a implementação concluída somente quando:

- o DTO externo estiver alinhado ao Swagger Pix do BACEN;
- o envelope Kafka estiver separado do payload Pix;
- valores e datas estiverem tipados corretamente;
- mensagens inválidas tiverem tratamento explícito;
- o processamento for idempotente por `endToEndId`;
- o fluxo continuar compatível com Remote Partitioning do Spring Batch;
- não houver exposição de dados pessoais nos logs;
- testes cobrirem o caminho feliz, opcionais, devoluções, retry e duplicidade;
- a documentação de configuração dos tópicos e propriedades Kafka estiver
  atualizada.
