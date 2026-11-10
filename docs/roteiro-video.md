# Roteiro dos vídeos de demonstração (até 3 minutos cada)

Os vídeos são individuais e devem ser gravados e narrados por você. Os tempos abaixo são sugestões. Antes de gravar, inicie a aplicação com `app.seed.pedidos-demo=true` em um banco próprio para ter dados no dashboard.

## Vídeo 1: Controle de acesso, pedidos e auditoria

| Tempo | O que mostrar | Sugestão de fala |
| --- | --- | --- |
| 0:00 a 0:20 | Tela de login e README | "Evoluí o PediuPatrão com três perfis, fluxo controlado de pedidos e auditoria persistente no MongoDB." |
| 0:20 a 0:50 | Login como atendente: menu reduzido; abrir `/usuarios` direto e receber 403 | "As permissões são verificadas no backend. Esconder o botão não basta: mesmo digitando a URL, o acesso é negado." |
| 0:50 a 1:30 | Registrar pedido; avançar até Em preparação; tentar finalizar pela API e receber 422 | "O total é calculado no servidor a partir dos preços cadastrados. Transições fora do fluxo são bloqueadas." |
| 1:30 a 2:10 | Login como gerente: desconto de 25% recusado, 10% aceito, cancelamento com justificativa | "Desconto e cancelamento são exclusivos do gerente, limitados a 20% e apenas antes da saída." |
| 2:10 a 2:50 | Login como admin: tela de auditoria com valor anterior e novo; verificar integridade | "Cada evento guarda usuário, perfil, data, campos alterados e justificativa. Os registros são encadeados por hash: qualquer alteração direta no banco é detectada." |
| 2:50 a 3:00 | Tela do `./mvnw test` | "Os testes automatizados cobrem as regras e as permissões." |

## Vídeo 2: Dashboard administrativo e API

| Tempo | O que mostrar | Sugestão de fala |
| --- | --- | --- |
| 0:00 a 0:25 | `docs/requisitos.md` | "Analisei o que o responsável pela pizzaria precisa acompanhar e transformei em sete requisitos funcionais e requisitos não funcionais de segurança e desempenho." |
| 0:25 a 1:10 | Dashboard: indicadores com variação, gráfico por período, horário de pico | "Os indicadores comparam com o período anterior de mesma duração. O gráfico por hora mostra o pico, útil para escalar a equipe." |
| 1:10 a 1:50 | Filtros: 7 e 90 dias, categoria, status; ranking por quantidade e faturamento | "Todos os filtros são aplicados no servidor, e o ranking pode ser ordenado por quantidade ou faturamento." |
| 1:50 a 2:30 | `docs/api-endpoints.md` e uma chamada no navegador, incluindo um erro 400 | "Projetei endpoints separados por necessidade, todos somente leitura e restritos a ADMIN e GERENTE, com erros padronizados." |
| 2:30 a 3:00 | Atualização automática e branches no GitHub | "A tela atualiza a cada 60 segundos e trata dados vazios e erros por gráfico. O código segue gitflow, com a branch feat/dashboard-api." |

## Decisões que valem ser justificadas na apresentação

1. **Endpoints separados por indicador** em vez de um único endpoint grande: cada gráfico carrega e falha de forma independente, e a API pode ser reutilizada por outros clientes.
2. **Cálculos em `DashboardCalculator`, sem dependência do Spring**: mais fáceis de testar; o banco faz apenas a filtragem, pelo campo indexado `dataHoraEntrada`.
3. **Cancelados fora do faturamento, mas dentro das contagens**: evita inflar receita sem esconder o problema operacional.
4. **Comparação com período anterior**: um número isolado diz pouco; a variação mostra tendência.
5. **Auditoria com hash encadeado**: atende ao requisito de integridade sem infraestrutura extra.
