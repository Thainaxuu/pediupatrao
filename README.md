# PediuPatrão

Sistema de pedidos para pizzaria em **Java 21, Spring Boot 3.4, Spring Security, MongoDB e Thymeleaf**, evoluído em duas atividades:

1. **Atividade Prática M1 (Engenharia de Software):** três perfis de acesso com autorização no backend, pedidos com itens e valores calculados no servidor, fluxo controlado de entrada e saída, descontos e cancelamentos exclusivos do gerente e auditoria persistente.
2. **Análise e Projeto de Software:** dashboard administrativo alimentado por uma API REST própria.

## Documentação

| Documento | Conteúdo |
| --- | --- |
| [docs/requisitos.md](docs/requisitos.md) | Requisitos funcionais e não funcionais, matriz de permissões, regras de cálculo e limitações |
| [docs/api-endpoints.md](docs/api-endpoints.md) | Endpoints com parâmetros, exemplos de requisição e resposta e erros possíveis |
| [docs/evidencias-testes.md](docs/evidencias-testes.md) | Testes automatizados e roteiro de testes manuais |
| [docs/roteiro-video.md](docs/roteiro-video.md) | Roteiro das demonstrações em vídeo e decisões de projeto |

## Como executar

Pré-requisitos: Java 21 e acesso a um MongoDB (o Atlas configurado pelo professor ou um local).

```bash
# Linux/Mac
./mvnw spring-boot:run
# Windows
mvnw.cmd spring-boot:run
```

Acesse `http://localhost:8080`.

### Banco próprio e dados de demonstração (recomendado)

O banco configurado por padrão (`pizzaria`) é compartilhado pela turma. Para demonstrar sem interferir no trabalho dos colegas e com dados no dashboard:

```bash
# Linux/Mac
MONGODB_DATABASE=pizzaria_seunome ./mvnw spring-boot:run -Dspring-boot.run.arguments=--app.seed.pedidos-demo=true
# Windows (PowerShell)
$env:MONGODB_DATABASE="pizzaria_seunome"; .\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--app.seed.pedidos-demo=true"
```

O gerador cria produtos, clientes e cerca de 60 dias de pedidos, apenas se ainda não houver pedidos com itens.

### Usuários iniciais

| Usuário | Senha | Perfil |
| --- | --- | --- |
| admin | teste123 (ou variável `ADMIN_PASSWORD`) | ADMIN |
| gerente01 | gerente123 | GERENTE |
| atendente01 | atendente123 | ATENDENTE |

Os usuários de demonstração podem ser desativados com `app.seed.usuarios-demo=false`. Troque as senhas em qualquer uso real.

### Testes

```bash
./mvnw test
```

78 testes automatizados (regras de negócio, auditoria, dashboard e autorização da API com MockMvc). Os testes não precisam de MongoDB. O GitHub Actions executa os testes a cada push.

## O que foi implementado

### Etapa 1: controle de acesso, pedidos e auditoria

| Requisito | Onde está |
| --- | --- |
| RF01 Perfis e permissões | `SecurityConfig` (regras por URL e método, CSRF, 401/403 em JSON), `@PreAuthorize` nos serviços, `UsuarioDetailsService`, `UsuarioService` (proteção contra autoelevação) |
| RF02 Pedido com itens e total no servidor | `Pedido`, `ItemPedido`, `NovoPedidoRequest`, `CalculadoraPedido`, `PedidoService.criarPedido` |
| RF03 Entrada, saída e transições | `StatusPedido` (máquina de estados), `PedidoService.atualizarStatus` |
| RF04 Cancelamento e desconto | `PedidoService.cancelar` e `PedidoService.aplicarDesconto` |
| RF05 Auditoria | `RegistroAuditoria`, `AuditoriaRepository` (somente leitura), `AuditoriaService` (hash encadeado), `AuditoriaController`, tela `/auditoria` |

Correções de segurança no código recebido: `/api/usuarios` era público (qualquer pessoa podia criar um ADMIN), o CSRF estava desativado, a API devolvia o hash da senha, o formulário de edição de usuário exibia o hash, a lista de clientes era vulnerável a XSS e todo usuário autenticado recebia o mesmo perfil.

### Etapa 2: dashboard administrativo

- Tela `/dashboard` (ADMIN e GERENTE) com indicadores, gráficos (Chart.js), ranking, situação dos pedidos, filtros e atualização automática.
- API `GET /api/dashboard/{resumo, vendas-por-periodo, vendas-por-hora, status, produtos-mais-vendidos, vendas-por-categoria, pedidos-recentes, filtros}`.
- Código: `DashboardController`, `DashboardService`, `dashboard/DashboardCalculator`, `dashboard/FiltroDashboardParser`, `static/js/dashboard.js`.

## Arquitetura

```
controller/          REST (/api/**)
controller/web/      Páginas Thymeleaf
service/             Regras de negócio e autorização por método
dashboard/           Cálculos e validação de filtros do dashboard (sem dependência de framework)
repository/          Spring Data MongoDB
entity/              Documentos e enums (Perfil, StatusPedido, TipoOperacao...)
dto/                 Contratos de entrada e saída da API
security/            Usuário logado e tratamento de acesso negado
exception/           Exceções de domínio e respostas de erro padronizadas
config/              Segurança, conversores, carga inicial e dados de demonstração
```

## Fluxo de trabalho (gitflow)

| Branch | Uso |
| --- | --- |
| `main` | Versões entregues (tag `v1.0.0`) |
| `develop` | Integração |
| `feat/controle-acesso-auditoria` | Etapa 1 |
| `feat/dashboard-api` | Etapa 2: API e dashboard |
| `release/1.0.0` | Preparação da entrega |

Para publicar no GitHub (crie antes um repositório vazio):

```bash
sh scripts/ajustar-autor.sh "Seu Nome" "seu.email@exemplo.com"   # opcional: commits com seu nome
git remote add origin https://github.com/SEU_USUARIO/pediupatrao.git
git push -u origin --all
git push origin --tags
```
