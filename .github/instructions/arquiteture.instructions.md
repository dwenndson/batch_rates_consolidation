# Instruções de Arquitetura para o Copilot (Spring Batch + Kafka)

> As regras gerais e decisões de conflito estão em
> `.github/copilot-instructions.md`.
> Profiles e configuração de infraestrutura estão em
> `runtime.instructions.md`.

## 1. Contexto do Projeto
Você atua como um assistente de IA especialista em arquitetura distribuída Java. Sua tarefa é guiar e gerar código para um sistema de processamento em lote baseado no padrão **Remote Partitioning** (Particionamento Remoto) utilizando Spring Boot 4.x, Spring Batch compatível e Apache Kafka. O domínio de negócio é a tarifação massiva de transações PIX.

Cada execução do batch deve ser iniciada exclusivamente por um endpoint REST
protegido. Não implementar disparos agendados ou qualquer mecanismo alternativo
ao endpoint REST.

## 2. Mapa Arquitetural (Fonte da Verdade)
Utilize o diagrama Mermaid abaixo como o guia definitivo para entender o fluxo de dados, as responsabilidades de cada componente e a comunicação assíncrona do sistema. Todo o código gerado deve refletir exatamente este fluxo:

```mermaid
graph TD
    %% Entidades Externas e Banco
    DB[(Banco de Dados Relacional\nTransacaoPix / ConsolidadoPix / JobRepository)]
    API[API Externa de Isenções\n(Simulação)]

    %% Nó Manager
    subgraph Manager_Node [Manager Node (Master)]
        Trigger((Gatilho do Batch\nEndpoint REST protegido))
        Partitioner[Partitioner\nDivide IDs em fatias]
        ReplyListener[Reply Listener\nAcompanha o Status]
    end

    %% Kafka
    subgraph Apache_Kafka [Apache Kafka Cluster]
        TopicReq>Tópico: batch-requests]
        TopicRep>Tópico: batch-replies]
    end

    %% Nós Workers
    subgraph Workers_Nodes [Worker Nodes (Escalável HZ)]
        Worker1[Worker 1\nLê Partição A]
        Worker2[Worker 2\nLê Partição B]
        WorkerN[Worker N\nLê Partição N]
    end

    %% Fluxo Manager
    Trigger --> Partitioner
    Partitioner -- "1. Lê metadados" --> DB
    Partitioner -- "2. Publica metadados das fatias" --> TopicReq
    
    %% Fluxo Workers
    TopicReq -- "3. Consomem intervalos de ID" --> Worker1
    TopicReq --> Worker2
    TopicReq --> WorkerN

    %% Ciclo do Worker 1 (Representando todos)
    Worker1 -- "4. ItemReader (Lê Chunk Pix)" --> DB
    Worker1 -- "5. ItemProcessor (Verifica Isenção)" --> API
    Worker1 -- "6. ItemWriter (Salva Tarifa)" --> DB
    Worker1 -- "7. Publica Status de Conclusão" --> TopicRep
    Worker2 --> TopicRep
    WorkerN --> TopicRep

    %% Fluxo de Retorno
    TopicRep -- "8. Confirma conclusão" --> ReplyListener
    ReplyListener -- "9. Integra com o JobRepository" --> DB

    classDef database fill:#f9f,stroke:#333,stroke-width:2px;
    classDef kafka fill:#ff9,stroke:#333,stroke-width:2px;
    classDef worker fill:#bbf,stroke:#333,stroke-width:2px;
    class DB,API database;
    class TopicReq,TopicRep,Apache_Kafka kafka;
    class Worker1,Worker2,WorkerN worker;
```

## 3. Diretivas de Implementação para o Código
Baseado no fluxograma acima, obedeça às seguintes regras ao gerar as classes:

Regras do Manager Node
Partitioner: Crie uma implementação de Partitioner que consulte a tabela TransacaoPix para pegar os IDs mínimo e máximo e dividi-los em intervalos (fatias).
Sem lógica de negócio: O Manager NÃO calcula tarifas nem lê/salva as entidades brutas. Ele apenas orquestra o Job e despacha as partições para o tópico batch-requests.

Reply Listener: Usar as abstrações suportadas de Remote Partitioning do Spring
Batch para correlacionar as respostas e atualizar o estado da execução. Não
alterar manualmente tabelas ou metadados internos do `JobRepository`.

Regras dos Worker Nodes
Consumo Kafka: O Worker atua como o Step escravo, consumindo mensagens do tópico batch-requests.

ItemReader: Configurado para buscar dados da entidade `TransacaoPix` usando
como filtro os IDs de início e fim (`minId` e `maxId`) recebidos pelos
metadados da partição oriunda do Kafka. O Kafka não transporta o payload Pix.

ItemProcessor: É aqui que ocorre a lógica. O código gerado deve simular uma chamada a um serviço REST (API Externa de Isenções) via FeignClient ou RestTemplate.

ItemWriter: Deve mapear o resultado para a entidade ConsolidadoPix e persistir no banco.

Feedback: Ao concluir o processamento do chunk, o Worker obrigatoriamente notifica o tópico batch-replies.