# Instruções para testes de sucesso

> Estas instruções complementam `.github/copilot-instructions.md`.

Ao criar um teste do fluxo bem-sucedido do Worker:

1. Use uma transação Pix válida e sem dados pessoais reais.
2. Persista a transação com status `PENDING`.
3. Execute o `workerProcessingStep` com uma partição que contenha o `id`
   persistido.
4. Verifique que existe um `ConsolidadoPix` para o mesmo `endToEndId`.
5. Verifique que a tarifa usa `BigDecimal` e a regra de isenção foi aplicada.
6. Verifique que a transação terminou com status `CONSOLIDATED`.
7. Execute a mesma partição novamente e verifique que não há segundo
   consolidado nem erro de restrição única.
8. Não valide o sucesso apenas por logs. Use o estado persistido e o status do
   Step ou do Job.

O teste deve isolar a API de isenções com um stub ou mock determinístico. Não
faça chamadas de rede em teste unitário e não inclua payload Pix completo em
mensagens Kafka.
