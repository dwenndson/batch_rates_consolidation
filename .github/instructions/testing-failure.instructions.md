# Instruções para testes de falha

> Estas instruções complementam `.github/copilot-instructions.md`.

Ao criar um teste de falha do Worker:

1. Faça a API de isenções lançar uma exceção controlada e sanitizada.
2. Execute o processamento com uma transação Pix válida, sem dados reais.
3. Verifique que o retry ocorre somente até o limite configurado para a falha
   transitória.
4. Verifique que o Step termina como `FAILED` quando o limite é excedido.
5. Verifique que a transação não gera `ConsolidadoPix` parcial.
6. Verifique que a mensagem de erro não contém CPF, CNPJ, nome, chave Pix,
   payload Pix, credenciais ou tokens.
7. Verifique que a resposta técnica contém apenas identificadores de execução,
   status, contadores e erro sanitizado.
8. Verifique que uma mensagem inválida não é confirmada como concluída e segue
   para retry e Dead Letter Topic (DLT), quando a integração Kafka estiver
   habilitada.
9. Verifique a retomada usando o `JobRepository`, sem alterar diretamente suas
   tabelas internas.

O teste deve provar o estado persistido e o status do Step ou do Job. Não
considere uma exceção capturada sem mudança de estado como um teste de falha
completo.
