# Escopo principal do projeto: microsserviço distribuído para consolidação de tarifas Pix

> Fonte canônica: `.github/copilot-instructions.md`. Este arquivo mantém
> somente o escopo geral; as regras especializadas ficam em
> `.github/instructions/`.

Para configuração de profiles, banco, `JobRepository`, Kafka e inicialização,
consultar também `instructions/runtime.instructions.md`.

## Índice das instruções

- `instructions/arquiteture.instructions.md`: fluxo Remote Partitioning e
  responsabilidades do Manager e do Worker.
- `instructions/runtime.instructions.md`: profiles, banco, `JobRepository`,
  Kafka e inicialização.
- `instructions/prompt.md`: contrato Pix, validações e idempotência.
- `instructions/observability-instructions.md`: Actuator, métricas e
  Prometheus.
- `instructions/humanization.instructions.md`: escrita clara, naturalidade e
  revisão de código sem padrões genéricos.
