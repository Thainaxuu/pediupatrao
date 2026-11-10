# Evidências de Testes

> **Como gerar as evidências:** execute `./mvnw test` (Linux/Mac) ou `mvnw.cmd test` (Windows) e capture a tela com o resumo `Tests run: ..., Failures: 0`. Os relatórios detalhados ficam em `target/surefire-reports/`. Ao enviar o projeto para o GitHub, o workflow em `.github/workflows/ci.yml` executa os mesmos testes automaticamente; o link da execução (aba **Actions**) e o artefato `relatorios-de-teste` servem como evidência pública e verificável.
>
> Preencha a coluna **Resultado** com o que você observar e substitua os marcadores de imagem pelas suas capturas de tela. Não registre resultados que não executou.

## 1. Testes automatizados

| Classe | O que verifica | Qtd. | Resultado |
| --- | --- | :---: | --- |
| `StatusPedidoTest` | Fluxo de entrega e de retirada, bloqueio de saltos e retornos, estados finais, conversão de status e perfis antigos | 7 | |
| `CalculadoraPedidoTest` | Subtotal, desconto com arredondamento, limite de 20% | 3 | |
| `PedidoServiceTest` | Total calculado no servidor, produto inativo, transições inválidas, registro de saída, cancelamento com justificativa, limites de desconto, chamadas à auditoria | 15 | |
| `UsuarioServiceTest` | Perfil válido, senha criptografada, proteção contra autoelevação, último ADMIN, senha nunca auditada | 7 | |
| `ClienteServiceTest` | Auditoria de endereço e telefone com valores anteriores e novos, telefone mascarado, validação, exclusão bloqueada | 4 | |
| `AuditoriaServiceTest` | Usuário, perfil, data e hora, encadeamento de hash, detecção de adulteração e de exclusão | 3 | |
| `DiferencasTest` | Comparação campo a campo e máscara de telefone | 3 | |
| `AutorizacaoApiTest` | Autorização no backend: 401 sem login, 403 sem CSRF, cancelamento e desconto só para GERENTE, usuários só para ADMIN, auditoria e dashboard negados ao ATENDENTE, senha ausente da resposta, 405 ao excluir auditoria, 400 em filtro inválido | 19 | |
| `DashboardCalculatorTest` | Indicadores, variação, série diária e semanal, fuso horário, status, rankings, filtro por categoria, dados incompletos | 10 | |
| `FiltroDashboardParserTest` | Período padrão, listas de status, agregação de erros, período inválido, período anterior, agrupamento, limites | 7 | |

`[INSERIR CAPTURA: resultado do ./mvnw test]`

`[INSERIR LINK: execução do GitHub Actions]`

## 2. Roteiro de testes manuais (demonstração)

Pré-requisito: aplicação iniciada com `app.seed.pedidos-demo=true` em banco próprio. Usuários: `admin/teste123`, `gerente01/gerente123`, `atendente01/atendente123`.

### Etapa 1: controle de acesso, pedidos e auditoria

| # | Passo | Resultado esperado | Resultado |
| --- | --- | --- | --- |
| M01 | Entrar como `atendente01` | Menu sem Usuários, Auditoria e Dashboard | |
| M02 | Abrir `/usuarios` direto pela barra de endereço como `atendente01` | Página 403 | |
| M03 | Cadastrar cliente e depois alterar o logradouro | Cliente salvo; alteração aparece na auditoria | |
| M04 | Registrar pedido com 2 itens | Total igual à soma dos preços cadastrados; status Recebido; entrada com usuário e horário | |
| M05 | No navegador (F12), enviar `POST /api/pedidos/{id}/cancelar` como `atendente01` | 403 em JSON | |
| M06 | Avançar o pedido até Em preparação e tentar `PUT .../status` com `FINALIZADO` | 422 "Transição inválida" | |
| M07 | Entrar como `gerente01`, aplicar desconto de 25% | 400 "limitado a 20%" | |
| M08 | Aplicar desconto de 10% com justificativa | Total recalculado; registro de desconto na trilha | |
| M09 | Cancelar com justificativa curta e depois com justificativa válida | 400 e depois pedido Cancelado | |
| M10 | Tentar avançar o status do pedido cancelado | Botões não aparecem; pela API, 422 | |
| M11 | Entrar como `admin`, abrir Auditoria e filtrar por CLIENTE | Registros com valor anterior e novo; telefone mascarado | |
| M12 | Clicar em "Verificar integridade" | "Todos os N registros estão íntegros" | |
| M13 | Como `admin`, tentar alterar o próprio perfil | Campo bloqueado; pela API, 422 | |

### Etapa 2: dashboard

| # | Passo | Resultado esperado | Resultado |
| --- | --- | --- | --- |
| D01 | Entrar como `gerente01` e abrir Dashboard | Indicadores, 5 gráficos e tabela carregados | |
| D02 | Clicar em "7 dias" e depois "90 dias" | Todos os dados mudam; agrupamento automático muda para semana | |
| D03 | Filtrar status Cancelado | Faturamento zero e taxa de cancelamento 100% | |
| D04 | Filtrar categoria Bebida | Ranking e categorias só com bebidas | |
| D05 | Alternar ranking para Faturamento | Ordem do ranking muda | |
| D06 | Definir data inicial posterior à final | Mensagem de erro sem quebrar a tela | |
| D07 | Registrar um pedido em outra aba e aguardar 60 s | Dashboard atualiza sozinho; horário de atualização muda | |
| D08 | Acessar `/api/dashboard/resumo?status=XPTO` | 400 com a lista de erros | |
| D09 | Entrar como `atendente01` e abrir `/dashboard` | Página 403 | |

`[INSERIR CAPTURAS DE TELA DOS PASSOS PRINCIPAIS]`
