# Instruções canônicas do projeto

Este arquivo é a fonte principal das decisões arquiteturais do projeto. Os
arquivos em `.github/instructions/` detalham a arquitetura, a execução, o
domínio Pix, a observabilidade e o padrão de escrita. Em caso de conflito,
este arquivo vence.

A instrução `humanization.instructions.md` orienta a escrita clara e a revisão
de código natural. Ela não autoriza inventar fatos, fontes, experiências,
resultados de teste ou comportamento de APIs.

## Objetivo

Construir um microsserviço de consolidação de tarifas Pix com Java 21, Spring
Boot, Spring Batch, Apache Kafka e PostgreSQL.

O processamento usa Remote Partitioning com dois perfis lógicos do mesmo
artefato:

- `manager`: recebe a solicitação REST, cria o Job, particiona o trabalho e
  acompanha a execução;
- `worker`: recebe uma partição, lê os dados no banco, aplica a regra de
  negócio e persiste o resultado.

## Regras arquiteturais obrigatórias

- O endpoint REST protegido é o único mecanismo de início de uma execução.
- Não criar agendamento, CRON ou fluxo legado baseado em execução periódica.
- O Manager não calcula tarifas, não aplica regras Pix e não persiste o
  consolidado.
- O Worker deve ser stateless e executável em múltiplas instâncias.
- O PostgreSQL é a fonte de verdade das transações e dos resultados.
- O `JobRepository` relacional guarda o estado do Job, Steps, partições e
  retomadas.
- Usar as abstrações oficiais de Remote Partitioning do Spring Batch.
- Não criar um listener que altere manualmente o estado interno do
  `JobRepository` fora do fluxo suportado pelo Spring Batch.

## Contrato da partição

O Kafka é usado exclusivamente pelo Remote Partitioning. Uma mensagem de
`batch-requests` deve transportar somente metadados técnicos:

- `jobExecutionId`;
- `stepExecutionId`;
- `partitionId`;
- `minId`;
- `maxId`.

O payload Pix completo não deve ser transportado pelo Kafka. O Worker deve
usar `minId` e `maxId` para ler os registros da partição no PostgreSQL. Isso
mantém uma única fonte de verdade, evita mensagens grandes e reduz a exposição
de dados pessoais.

O `partitionId` é a chave Kafka da instrução. O tópico `batch-replies` deve
transportar os identificadores técnicos, o status, os contadores e um erro
sanitizado quando houver falha. Nunca incluir payload Pix ou dados pessoais na
resposta.

## Consistência e falhas

- O sistema deve assumir entrega at-least-once, não exactly-once.
- A confirmação da mensagem só pode ocorrer depois do commit do processamento
  da partição.
- O Writer deve ser idempotente e possuir restrição única para `endToEndId`.
- Uma resposta duplicada não pode concluir uma partição duas vezes.
- Dados divergentes para o mesmo `endToEndId` devem gerar conflito explícito,
  nunca sobrescrita silenciosa.
- Retry deve ser limitado e aplicado somente a falhas transitórias.
- Mensagens inválidas devem seguir para retry e Dead Letter Topic (DLT).
- A retomada deve usar o estado persistido no `JobRepository`.
- Falhas de publicação de resposta devem ser tratadas sem marcar a partição
  como concluída prematuramente.

## Domínio Pix

- Manter o DTO externo separado do envelope Kafka.
- Seguir o contrato oficial do Banco Central do Brasil e aceitar campos
  adicionais compatíveis.
- `endToEndId` é obrigatório e identifica a transação para idempotência.
- Valores monetários usam `BigDecimal`, com escala e arredondamento definidos.
- Datas usam `OffsetDateTime` ou `Instant`.
- Campos opcionais do contrato Pix não devem ser transformados em obrigatórios
  sem justificativa de domínio.
- A conversão para o modelo interno ocorre em um mapper explícito.

## Segurança e privacidade

- Não registrar CPF, CNPJ, nome, chave Pix, informação livre, payload Pix,
  credenciais ou tokens.
- Credenciais, URLs e limites operacionais devem vir de configuração externa.
- Proteger o endpoint de execução com autenticação e autorização explícitas.
- Expor somente os endpoints Actuator necessários.
- Logs devem usar identificadores técnicos e dados de baixa sensibilidade.

## Ordem de implementação

1. Configuração externa, profiles e conexão com PostgreSQL.
2. Migrations e `JobRepository` compartilhado.
3. Entidades, mapeamentos, repositories e restrições de idempotência.
4. Contratos do Remote Partitioning.
5. Job do Manager e Partitioner.
6. Worker, `ItemReader`, `ItemProcessor` e `ItemWriter`.
7. Integração com a API de isenções, timeout e retry.
8. DLT, retomada, respostas e observabilidade.
9. Testes unitários, testes de integração e documentação operacional.

Cada etapa deve manter o projeto compilável e os testes existentes passando.
