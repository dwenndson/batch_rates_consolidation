# Instruções de escrita e código natural

Estas instruções valem para respostas, documentação, mensagens de commit,
comentários, nomes e código gerados ou revisados neste projeto.

## Objetivo

Escrever de forma clara, específica e compatível com o contexto do projeto.
Evitar padrões genéricos que fazem um texto parecer produzido por um modelo de
linguagem. Naturalidade não significa inventar uma voz pessoal, esconder a
origem do conteúdo ou alterar o comportamento do sistema.

## Roteiro para textos

Antes de escrever:

1. Identifique o leitor, o objetivo e a decisão que precisa ser comunicada.
2. Use os termos reais do projeto e os fatos disponíveis.
3. Remova informações que não ajudam o leitor a entender ou decidir.

Durante a revisão:

- Comece pelo fato ou pela ação principal. Corte saudações vazias, elogios,
  introduções genéricas e frases como “é importante destacar”.
- Troque abstrações por fatos observáveis, nomes de componentes, valores,
  estados, causas e consequências.
- Não aumente a importância de um assunto com expressões como “marco
  significativo”, “papel fundamental”, “legado duradouro”, “cenário em
  constante evolução” ou equivalentes sem evidência concreta.
- Diga a relação diretamente. Prefira “o Worker lê do PostgreSQL” a
  “o Worker está associado ao PostgreSQL”.
- Não atribua uma opinião a “especialistas”, “fontes”, “estudos” ou “o
  mercado” sem identificar e conferir a fonte.
- Não transforme uma ou duas referências em consenso geral.
- Remova análises superficiais no fim da frase, especialmente construções com
  gerúndio que apenas repetem a importância do fato.
- Evite listas de três itens usadas apenas para dar ritmo. Mantenha uma lista
  quando ela representar uma decisão ou um conjunto real.
- Não crie seções previsíveis como “Desafios”, “Perspectivas futuras” ou
  “Impacto” se o conteúdo não exigir essa divisão.
- Use negrito, títulos, tabelas e blocos de código apenas quando melhorarem a
  leitura. Não use Markdown decorativo.
- Varie o tamanho das frases sem criar frases fragmentadas ou slogans.
- Prefira voz ativa e elimine construções passivas quando o agente for
  conhecido.
- Evite advérbios e palavras de intensidade que não acrescentem precisão.
- Não use travessão longo. Reescreva a frase ou use pontuação simples.
- Não inclua comentários sobre o próprio processo, como “a seguir vou
  explicar”, “espero que ajude” ou “esta é a melhor abordagem”, salvo quando
  essa informação for necessária para uma decisão.

## Roteiro para código

Código natural deve parecer parte do projeto e seguir suas decisões técnicas:

- Preserve os nomes de domínio usados pelo sistema, como `Pix`,
  `endToEndId`, `Worker` e `JobRepository`.
- Escolha nomes específicos para classes, métodos e variáveis. Evite nomes
  genéricos como `processData`, `handleThing` e `doIt`.
- Prefira funções pequenas, fluxo direto e abstrações justificadas pelo
  domínio. Não crie camadas, interfaces ou padrões apenas para parecer
  sofisticado.
- Não adicione comentários que repetem o código. Comente somente uma regra de
  negócio, limitação externa ou decisão que não seja evidente.
- Remova código morto, exemplos fictícios, `TODO` sem contexto e comentários
  que narram o histórico da alteração.
- Mantenha tratamento de erro, validação, logs e testes alinhados ao padrão
  existente. Não silencie exceções para deixar o código mais “limpo”.
- Não altere nomes, formatação ou estrutura apenas para variar o estilo.
  Consistência com o repositório é mais importante que variedade.
- Não invente bibliotecas, endpoints, métricas, fontes, resultados de teste ou
  comportamento de uma API.

## Checklist antes de entregar

- O primeiro parágrafo ou método deixa claro o resultado?
- Cada afirmação importante tem uma base verificável?
- Há frases genéricas, promocionais ou vagas que podem ser removidas?
- Alguma fonte foi citada sem ter sido consultada?
- O texto usa listas, negrito ou títulos por necessidade real?
- O código usa nomes do domínio e mantém o padrão do projeto?
- Os comentários explicam somente o motivo de uma decisão?
- Testes e validações demonstram o comportamento descrito?

Não use ferramentas de detecção de texto por inteligência artificial como
prova. A referência da Wikipédia é descritiva e deve orientar revisão de
qualidade, não servir para mascarar fatos, autoria ou decisões:

https://en.wikipedia.org/wiki/Wikipedia:Signs_of_AI_writing

As regras gerais do projeto e as decisões arquiteturais continuam tendo
precedência quando houver conflito.
