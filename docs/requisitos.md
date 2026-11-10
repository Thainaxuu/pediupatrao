# Requisitos do Sistema PediuPatrão

Este documento reúne os requisitos funcionais e não funcionais das duas atividades:

1. **Atividade Prática M1 (Engenharia de Software):** controle de acesso, gestão de pedidos e auditoria.
2. **Atividade de Análise e Projeto de Software:** dashboard administrativo integrado por API.

## 1. Atores e perfis

| Perfil | Responsabilidades |
| --- | --- |
| ADMIN | Administra usuários e perfis, consulta pedidos, visualiza auditoria e dashboard. |
| GERENTE | Gerencia pedidos, aplica descontos, cancela pedidos, consulta clientes, gerencia produtos, visualiza auditoria e dashboard. |
| ATENDENTE | Cadastra e atualiza clientes, registra pedidos e acompanha o andamento. |

### Matriz de permissões (aplicada no backend)

| Operação | ADMIN | GERENTE | ATENDENTE |
| --- | :---: | :---: | :---: |
| Criar, alterar e excluir usuários | ✔ | ✘ | ✘ |
| Consultar pedidos | ✔ | ✔ | ✔ |
| Registrar pedido | ✘ | ✔ | ✔ |
| Avançar status do pedido | ✘ | ✔ | ✔ |
| Aplicar desconto | ✘ | ✔ | ✘ |
| Cancelar pedido | ✘ | ✔ | ✘ |
| Consultar clientes | ✘ | ✔ | ✔ |
| Cadastrar e alterar clientes | ✘ | ✘ | ✔ |
| Excluir cliente (somente sem pedidos) | ✘ | ✔ | ✘ |
| Consultar produtos | ✔ | ✔ | ✔ |
| Cadastrar e alterar produtos | ✘ | ✔ | ✘ |
| Consultar auditoria | ✔ | ✔ | ✘ |
| Dashboard administrativo | ✔ | ✔ | ✘ |

Decisões de interpretação: o enunciado diz que somente o GERENTE aplica descontos e cancela pedidos, "inclusive quando comparado ao ADMIN", portanto o ADMIN apenas consulta pedidos. A exclusão de clientes não constava no enunciado; ela foi restrita ao GERENTE e bloqueada quando o cliente possui pedidos, para preservar o histórico. A manutenção de produtos (preços) foi atribuída ao GERENTE por ser informação comercial.

## 2. Requisitos funcionais da Atividade M1

**RF01. Controle de usuários e permissões**

- Três perfis (ADMIN, GERENTE, ATENDENTE) gravados no usuário e convertidos em roles do Spring Security no login.
- Usuários antigos com perfil `USER` ou sem perfil recebem o perfil de menor privilégio (ATENDENTE).
- Autorização em duas camadas: regras por URL e método HTTP no `SecurityConfig` e `@PreAuthorize` nos serviços.
- Proteção contra atribuição indevida de privilégios: só o ADMIN cria ou altera usuários; o perfil é validado no servidor; o ADMIN não altera o próprio perfil, não exclui a própria conta e o sistema nunca fica sem administrador.
- A rota `/api/usuarios`, antes pública, passou a ser exclusiva do ADMIN, e as respostas não expõem a senha.

**RF02. Evolução do cadastro de pedidos**

- O pedido registra itens (produto, quantidade, preço unitário e subtotal), percentual e valor de desconto, valor total, responsável pelo registro e data/hora de entrada.
- O cliente da API informa apenas produto e quantidade. Preços vêm do cadastro de produtos e o total é calculado no servidor (`CalculadoraPedido`).
- Validações: cliente existente, ao menos um item, quantidade de 1 a 99, produto ativo e com preço válido, endereço obrigatório para entrega.

**RF03. Controle de entrada, saída e transições**

Fluxo obrigatório: `RECEBIDO → EM_PREPARACAO → PRONTO → SAIU_PARA_ENTREGA (entrega) ou RETIRADO (balcão) → FINALIZADO`.

- A entrada registra data/hora e usuário responsável.
- A saída (SAIU_PARA_ENTREGA ou RETIRADO) registra obrigatoriamente data/hora e responsável.
- Transições fora do fluxo são recusadas (HTTP 422), por exemplo finalizar um pedido em preparação.
- Pedidos FINALIZADOS ou CANCELADOS não aceitam nova alteração de status.
- Cada mudança fica no histórico do próprio pedido e na auditoria.

**RF04. Cancelamentos e descontos**

- Exclusivos do GERENTE.
- Cancelamento: exige justificativa (mínimo de 10 caracteres), só é possível antes da saída, preserva itens e valores e impede alterações posteriores.
- Desconto: percentual de 0% a 20% (0% remove o desconto), aplicado somente antes da saída, com justificativa. O total é recalculado no servidor.
- Ambas as operações são auditadas com usuário, data/hora, valores anteriores e novos.

**RF05. Auditoria**

- Coleção própria `auditoria` no MongoDB, com entidade `RegistroAuditoria`, repositório `AuditoriaRepository` e serviço `AuditoriaService`.
- Eventos auditados: inclusão de pedido, mudança de status, desconto, cancelamento, inclusão, alteração e exclusão de clientes (endereço e telefone inclusos), alterações de produtos e usuários e tentativas de acesso negado.
- Cada registro guarda usuário, perfil, data e hora, operação, entidade e identificador, campos alterados com valor anterior e novo e justificativa quando exigida.
- Consulta exclusiva de ADMIN e GERENTE, com filtros e paginação.
- Integridade: não existe operação de alteração ou exclusão no repositório, no serviço ou na API. Cada registro guarda o hash SHA-256 do seu conteúdo encadeado ao registro anterior; o endpoint de integridade detecta alterações ou exclusões feitas diretamente no banco.
- Proteção de dados: senhas e hashes nunca são registrados (apenas "[REDEFINIDA]"); telefones são gravados mascarados, mantendo os quatro últimos dígitos; o log da aplicação não registra corpo de requisição.

## 3. Requisitos funcionais do Dashboard Administrativo

| Código | Requisito | Implementação |
| --- | --- | --- |
| RF01 | Indicadores gerais de vendas | Faturamento, total de pedidos, ticket médio, taxa de cancelamento, descontos concedidos, itens vendidos, tempo médio até a saída e variação percentual em relação ao período anterior de mesma duração (`/api/dashboard/resumo`). |
| RF02 | Análise de vendas (gráficos) | Faturamento e pedidos por dia, semana ou mês, com períodos sem venda preenchidos com zero (`/vendas-por-periodo`); pedidos por hora do dia com destaque do horário de pico (`/vendas-por-hora`); vendas por categoria (`/vendas-por-categoria`). |
| RF03 | Situação dos pedidos | Quantidade e percentual por status, incluindo status sem pedidos, e contadores dos pedidos em andamento (`/status`). |
| RF04 | Produtos mais vendidos | Ranking por quantidade ou por faturamento, com participação percentual (`/produtos-mais-vendidos`). |
| RF05 | Filtros de consulta | Período (com atalhos Hoje, 7, 30 e 90 dias e Este mês), status, tipo de entrega, categoria e produto. Os filtros valem para todos os indicadores e gráficos. |
| RF06 | Integração com a API | A tela consome exclusivamente endpoints REST próprios, em chamadas paralelas, com sessão autenticada e perfil verificado no servidor. |
| RF07 | Atualização e tratamento de dados | Atualização automática a cada 60 segundos (pausada com a aba oculta), botão de atualização manual, horário da última atualização, estados de carregamento, vazio e erro por gráfico, descarte de respostas atrasadas e tratamento de sessão expirada. No servidor: validação de parâmetros com mensagens claras, valores nulos tratados, pedidos antigos sem itens não quebram os cálculos e fuso horário de São Paulo nos agrupamentos. |

Regras de cálculo: faturamento, ticket médio, rankings e gráficos de vendas desconsideram pedidos cancelados; contagens de pedidos consideram todos. Ticket médio = faturamento ÷ pedidos válidos.

## 4. Requisitos não funcionais

| Código | Categoria | Requisito |
| --- | --- | --- |
| RNF01 | Tecnologia | Java 21, Spring Boot 3.4, Spring Security, Spring Data MongoDB e Thymeleaf, mantendo a arquitetura em camadas (entidades, repositórios, serviços e controladores). |
| RNF02 | Segurança | Autenticação por formulário com senha em BCrypt; autorização no backend por URL e por método; CSRF habilitado em formulários e chamadas `fetch`; sessão migrada no login (proteção contra fixação de sessão). |
| RNF03 | Segurança | API sem sessão responde 401 em JSON; acesso negado responde 403 em JSON e é registrado em log e auditoria. |
| RNF04 | Segurança | Prevenção de XSS: dados exibidos via JavaScript passam por `escapeHtml`; Thymeleaf escapa por padrão. |
| RNF05 | Privacidade (LGPD) | Minimização de dados pessoais na auditoria e ausência de credenciais em logs. |
| RNF06 | Integridade | Auditoria somente inclusão com encadeamento de hash; controle de concorrência otimista (`@Version`) no pedido, respondendo 409 em conflito. |
| RNF07 | Precisão | Valores monetários em `BigDecimal`, persistidos como Decimal128, com arredondamento bancário (HALF_EVEN) e duas casas. |
| RNF08 | Desempenho | Consultas do dashboard filtradas no banco por `dataHoraEntrada` (campo indexado) e limitadas a 366 dias por requisição. |
| RNF09 | Usabilidade | Interface responsiva (Bootstrap 5), mensagens de erro em português e botões exibidos conforme o perfil (a regra real fica no backend). |
| RNF10 | Manutenibilidade | Regras de negócio nos serviços; cálculos isolados em classes sem dependência de framework (`CalculadoraPedido`, `DashboardCalculator`); testes automatizados; padrão de erro único (`ErroResponse`). |
| RNF11 | Compatibilidade | Pedidos e usuários gravados pela versão anterior continuam legíveis (status e perfis antigos são convertidos). |
| RNF12 | Versionamento | Git com fluxo gitflow (`main`, `develop`, `feature/*`, `release/*`) e tag de versão. |

## 5. Limitações conhecidas

- A auditoria e a alteração do pedido não ocorrem em uma única transação do MongoDB (exigiria replica set e gerenciador transacional). Se a gravação da auditoria falhar, a operação retorna erro, mas a alteração já gravada permanece.
- A sequência da auditoria é garantida para uma única instância da aplicação (método sincronizado). Em execução com várias instâncias seria necessário um contador atômico no banco.
- O banco configurado por padrão é compartilhado pela turma. Recomenda-se usar um banco próprio pela variável `MONGODB_DATABASE`.
