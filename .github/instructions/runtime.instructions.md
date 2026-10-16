# Instruções de execução e configuração

As regras gerais estão em `.github/copilot-instructions.md`. Este arquivo
define somente a configuração comum do artefato e dos profiles.

## Profiles

O mesmo artefato deve ser executado com um profile explícito:

- `manager`: endpoint REST protegido, JobLauncher, Job do Manager, Partitioner
  e integração de saída das respostas;
- `worker`: consumo de `batch-requests`, Step Worker, leitura do banco,
  processamento e publicação em `batch-replies`.

Não ativar `manager` e `worker` juntos por padrão. Beans específicos devem ser
protegidos por `@Profile` ou configuração equivalente. Componentes comuns,
como datasource, transações, serialização e `JobRepository`, ficam fora dos
profiles.

Em produção, exigir um profile explícito. Sem profile, não expor a API do
Manager nem iniciar consumidores do Worker. O profile de teste pode ativar
somente os componentes necessários ao teste.

Nenhum profile pode iniciar Jobs automaticamente. O disparo ocorre somente
quando o endpoint protegido do Manager for chamado.

## Banco e JobRepository

- PostgreSQL é o banco padrão de execução e de negócio.
- Manager e Worker devem usar o mesmo `JobRepository` relacional e versões
  compatíveis do schema.
- A criação e a atualização do schema devem ser controladas por migration.
- Não usar `spring.batch.jdbc.initialize-schema=always` em ambiente produtivo.
- O profile de teste deve fornecer um banco reproduzível por Testcontainers ou
  por um banco embutido explicitamente configurado.
- A aplicação não deve depender de banco implícito para iniciar em produção.
- URL, usuário, senha, pool e limites de conexão devem vir de variáveis de
  ambiente ou de um gerenciador de segredos.

## Kafka

As propriedades devem ser configuráveis por ambiente:

- `bootstrap.servers`;
- tópico `batch-requests`;
- tópico `batch-replies`;
- grupo dos Workers;
- serializers e deserializers;
- política de confirmação;
- retry e Dead Letter Topic (DLT);
- limites de lote e tamanho de mensagem.

Usar tipos confiáveis e explicitamente autorizados no desserializador JSON.
Mensagens de partição usam `partitionId` como chave. Não aumentar os limites de
mensagem para transportar payload Pix; o payload é lido no PostgreSQL.

## Inicialização e Actuator

- Desabilitar a execução automática de Jobs na inicialização com
  `spring.batch.job.enabled=false`.
- Separar liveness de readiness.
- Readiness deve depender de PostgreSQL e Kafka quando o profile precisar
  desses recursos.
- Expor apenas os endpoints Actuator necessários e protegê-los conforme o
  ambiente.
- Não usar configurações locais, credenciais ou tópicos fixos em código Java.
