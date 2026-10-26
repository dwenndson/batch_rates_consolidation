# Codex instructions

## End-to-end validation

Use this procedure only on a machine with a running Docker daemon. The current
development machine has no Docker daemon, so do not report an end-to-end test
as passed here.

The objective is to prove the complete flow:

`REST Manager -> JobRepository -> batch-requests -> Worker -> PostgreSQL ->
batch-replies -> Manager`

Do not replace the real flow with mocks when executing this test. Unit tests
may mock the exemption service, but this procedure must use an HTTP stub that
the Worker reaches through the network.

### Preconditions

- Docker Compose is available and `docker info` succeeds.
- Java 21 and the Maven Wrapper are available.
- No real Pix payload or personal data is used.
- The Git working tree is clean before the test, or all test changes are
  clearly reported.

Create local environment values from `.env.example`:

```bash
cp .env.example .env
```

The Worker must not use `http://localhost:8090` for the exemption API when it
runs in a container. Run a disposable HTTP stub on the host and override the
Worker URL with:

```bash
EXEMPTION_API_BASE_URL=http://host.docker.internal:8090
```

The stub must implement:

- `GET /exemptions/{endToEndId}` with
  `{"exempt": false}` for the success scenario;
- a controlled HTTP 500 response for the failure scenario.

Do not log or return the complete request payload. The Worker sends only the
`endToEndId`.

### Start and readiness

If a previous PostgreSQL volume exists from an earlier run, remove old
containers and volumes first so Flyway initializes the database cleanly:

```bash
docker compose down -v
```

Run the stack in the background (rebuilding images to include
`spring-boot-starter-flyway` and `V3__seed_initial_pix_transactions.sql`):

```bash
docker compose --env-file .env up --build -d
```

Check the services before inserting data:

```bash
docker compose ps
curl --fail http://localhost:8080/actuator/health
curl --fail http://localhost:8081/actuator/health
docker compose exec -T postgres pg_isready -U "$POSTGRES_USER" -d batch_rates
```

Confirm that Flyway applied migrations `V1`, `V2`, and `V3` and that
`transacao_pix` already contains the seeded `PENDING` transactions
(`E2E-SEED-0001`, `E2E-SEED-0002`, `E2E-SEED-0003`):

```bash
docker compose exec -T postgres psql \
  -U "$POSTGRES_USER" \
  -d batch_rates <<'SQL'
SELECT version, script, success
FROM flyway_schema_history
ORDER BY installed_rank;

SELECT end_to_end_id, txid, valor, status
FROM transacao_pix
ORDER BY id;
SQL
```

Do not continue while PostgreSQL, Kafka, Manager, Worker, the Flyway migrations,
or the exemption stub is unavailable.

### Success scenario

Migration `V3__seed_initial_pix_transactions.sql` already seeds three synthetic
pending transactions (`E2E-SEED-0001`, `E2E-SEED-0002`, `E2E-SEED-0003`). You
may also insert an additional synthetic pending transaction:

```bash
docker compose exec -T postgres psql \
  -U "$POSTGRES_USER" \
  -d batch_rates <<'SQL'
INSERT INTO transacao_pix (
    end_to_end_id,
    valor,
    horario,
    pagador,
    devolucoes,
    status
) VALUES (
    'E2E-SUCCESS-0001',
    10.00,
    CURRENT_TIMESTAMP,
    '{}'::jsonb,
    '[]'::jsonb,
    'PENDING'
)
ON CONFLICT (end_to_end_id) DO NOTHING;
SQL
```

Start the Job:

```bash
curl --fail-with-body --user "$MANAGER_USER:$MANAGER_PASSWORD" \
  --header 'Content-Type: application/json' \
  --data '{"partitionSize":1}' \
  --request POST \
  http://localhost:8080/api/v1/batches/pix-consolidation/executions
```

Use the returned `jobExecutionId` to wait for `COMPLETED` in
`BATCH_JOB_EXECUTION`. Then assert all of the following:

```sql
SELECT end_to_end_id, status FROM transacao_pix
WHERE end_to_end_id IN (
    'E2E-SEED-0001',
    'E2E-SEED-0002',
    'E2E-SEED-0003',
    'E2E-SUCCESS-0001'
)
ORDER BY end_to_end_id;

SELECT end_to_end_id, tarifa, status
FROM consolidado_pix
WHERE end_to_end_id IN (
    'E2E-SEED-0001',
    'E2E-SEED-0002',
    'E2E-SEED-0003',
    'E2E-SUCCESS-0001'
)
ORDER BY end_to_end_id;

SELECT status, exit_code
FROM BATCH_JOB_EXECUTION
WHERE job_execution_id = :jobExecutionId;
```

Expected results:

- `transacao_pix.status` is `CONSOLIDATED` for the seeded rows and
  `E2E-SUCCESS-0001`;
- one `consolidado_pix` row exists per processed `end_to_end_id` with
  `tarifa = 1.00`;
- `consolidado_pix.status` is `COMPLETED`;
- the Job and its partition Steps are `COMPLETED`;
- no Pix payload appears in Kafka messages or logs.

Run the same partition or a retry and confirm that the unique `end_to_end_id`
constraint prevents a duplicate consolidated row.

### Failure scenario

Configure the exemption stub to return HTTP 500 for
`E2E-FAILURE-0001`, insert a pending transaction with that identifier, and
start the Job again.

Assert:

- the exemption request is retried only up to the configured limit;
- the partition Step and Job end as `FAILED`;
- no `consolidado_pix` row exists for `E2E-FAILURE-0001`;
- the transaction is not marked `CONSOLIDATED`;
- logs and the Job exit message contain no CPF, CNPJ, name, Pix key, payload,
  credential, or token;
- a malformed Kafka request is retried and reaches `batch-requests.DLT`;
- a malformed Kafka reply is retried and reaches `batch-replies.DLT`.

### Evidence to collect

Save only sanitized evidence:

- `docker compose ps`;
- health responses;
- Job and Step status;
- row counts and status values from PostgreSQL;
- Kafka topic names and offsets;
- relevant metrics from `/actuator/prometheus`;
- sanitized failure messages.

Never save credentials, tokens, complete Pix payloads, or personal data.

### Cleanup

After the test:

```bash
docker compose down -v
```

Stop the exemption stub and remove only its temporary files. Report the exact
commands used, the scenario results, and any environment limitation.
