# Documentação da API REST

Base: `http://localhost:8080`. Todas as rotas `/api/**` exigem sessão autenticada (login pelo formulário em `/login`). Requisições que alteram dados (`POST`, `PUT`, `DELETE`) exigem o token CSRF no cabeçalho `X-CSRF-TOKEN` (as páginas do sistema já enviam o token automaticamente pela função `apiFetch`).

## Padrão de erro

Todas as respostas de erro da API seguem o mesmo formato:

```json
{
  "timestamp": "2026-09-23T17:35:00.123Z",
  "status": 422,
  "erro": "UNPROCESSABLE_ENTITY",
  "mensagem": "Transição inválida: EM_PREPARACAO → FINALIZADO. Próximo status permitido: PRONTO.",
  "detalhes": [],
  "path": "/api/pedidos/66f1.../status"
}
```

| Código | Quando ocorre |
| --- | --- |
| 400 Bad Request | Dados ou parâmetros inválidos. `detalhes` lista todos os problemas encontrados. |
| 401 Unauthorized | Requisição sem sessão autenticada. |
| 403 Forbidden | Perfil sem permissão ou token CSRF ausente/inválido. O evento é registrado na auditoria. |
| 404 Not Found | Recurso inexistente. |
| 405 Method Not Allowed | Método não suportado (por exemplo `DELETE` na auditoria). |
| 409 Conflict | O registro foi alterado por outro usuário ao mesmo tempo (controle otimista) ou registro duplicado. |
| 422 Unprocessable Entity | Regra de negócio violada (transição inválida, desconto após a saída, cliente com pedidos etc.). |
| 500 Internal Server Error | Erro inesperado (detalhes apenas no log do servidor). |

---

# Parte 1: API do Dashboard Administrativo

Perfis: **ADMIN** e **GERENTE**. Todos os endpoints são `GET` (somente leitura).

### Parâmetros comuns de filtro

| Parâmetro | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `de` | data `AAAA-MM-DD` | Não | Início do período (inclusivo). Padrão: 29 dias antes de `ate`. |
| `ate` | data `AAAA-MM-DD` | Não | Fim do período (inclusivo). Padrão: hoje. |
| `status` | lista de status | Não | Repetido (`status=A&status=B`) ou separado por vírgula (`status=A,B`). Valores: `RECEBIDO`, `EM_PREPARACAO`, `PRONTO`, `SAIU_PARA_ENTREGA`, `RETIRADO`, `FINALIZADO`, `CANCELADO`. |
| `tipoEntrega` | texto | Não | `ENTREGA` ou `RETIRADA`. |
| `categoria` | texto | Não | Categoria do produto (ex.: `Pizza`). Seleciona pedidos com itens da categoria; rankings consideram só esses itens. |
| `produtoId` | texto | Não | Seleciona pedidos que contêm o produto. |

Regras: `de` não pode ser posterior a `ate`; período máximo de 366 dias; datas no fuso `America/Sao_Paulo`. Todos os erros de filtro são devolvidos juntos em um único 400.

### GET /api/dashboard/resumo (RF01)

Indicadores do período e comparação com o período anterior de mesma duração.

Requisição:
```
GET /api/dashboard/resumo?de=2026-09-01&ate=2026-09-30
```
Resposta 200:
```json
{
  "faturamento": 18452.30,
  "totalPedidos": 312,
  "pedidosValidos": 294,
  "pedidosCancelados": 18,
  "pedidosEmAndamento": 6,
  "ticketMedio": 62.76,
  "totalDescontos": 412.15,
  "itensVendidos": 845,
  "taxaCancelamento": 5.77,
  "tempoMedioAteSaidaMinutos": 37,
  "faturamentoPeriodoAnterior": 16210.00,
  "pedidosPeriodoAnterior": 280,
  "variacaoFaturamento": 13.83,
  "variacaoPedidos": 11.43
}
```
`variacaoFaturamento` e `variacaoPedidos` são `null` quando o período anterior não tem dados. `tempoMedioAteSaidaMinutos` é `null` quando nenhum pedido saiu no período.

### GET /api/dashboard/vendas-por-periodo (RF02)

| Parâmetro extra | Descrição |
| --- | --- |
| `agrupamento` | `DIA`, `SEMANA` (inicia na segunda-feira) ou `MES`. Padrão automático: dia até 62 dias, semana até 180, mês acima disso. |

```
GET /api/dashboard/vendas-por-periodo?de=2026-09-21&ate=2026-09-23&agrupamento=DIA
```
```json
[
  { "periodo": "2026-09-21", "rotulo": "21/09", "faturamento": 870.40, "pedidos": 14, "ticketMedio": 62.17 },
  { "periodo": "2026-09-22", "rotulo": "22/09", "faturamento": 0.00,   "pedidos": 0,  "ticketMedio": 0.00 },
  { "periodo": "2026-09-23", "rotulo": "23/09", "faturamento": 512.90, "pedidos": 8,  "ticketMedio": 64.11 }
]
```
Erro 400 com `agrupamento=ANO`: `"agrupamento inválido: 'ANO'. Use DIA, SEMANA ou MES."`

### GET /api/dashboard/vendas-por-hora (RF02)

Sempre 24 posições (0 a 23h, horário de Brasília), para identificar horários de pico.
```json
[ { "hora": 0, "pedidos": 0, "faturamento": 0.00 }, "...", { "hora": 20, "pedidos": 64, "faturamento": 4012.80 } ]
```

### GET /api/dashboard/status (RF03)

Todos os status são retornados, inclusive com quantidade zero.
```json
[
  { "status": "RECEBIDO", "descricao": "Recebido", "quantidade": 2, "percentual": 0.64 },
  { "status": "EM_PREPARACAO", "descricao": "Em preparação", "quantidade": 3, "percentual": 0.96 },
  { "status": "FINALIZADO", "descricao": "Finalizado", "quantidade": 288, "percentual": 92.31 },
  { "status": "CANCELADO", "descricao": "Cancelado", "quantidade": 18, "percentual": 5.77 }
]
```

### GET /api/dashboard/produtos-mais-vendidos (RF04)

| Parâmetro extra | Descrição |
| --- | --- |
| `limite` | 1 a 50. Padrão 10. |
| `ordenarPor` | `QUANTIDADE` (padrão) ou `FATURAMENTO`. |

```
GET /api/dashboard/produtos-mais-vendidos?limite=3&ordenarPor=FATURAMENTO&categoria=Pizza
```
```json
[
  { "posicao": 1, "produtoId": "66f0a1...", "nome": "Calabresa", "categoria": "Pizza", "quantidade": 120, "faturamento": 5988.00, "participacaoFaturamento": 31.20 },
  { "posicao": 2, "produtoId": "66f0a2...", "nome": "Portuguesa", "categoria": "Pizza", "quantidade": 80, "faturamento": 4392.00, "participacaoFaturamento": 22.88 },
  { "posicao": 3, "produtoId": "66f0a3...", "nome": "Mussarela", "categoria": "Pizza", "quantidade": 85, "faturamento": 3816.50, "participacaoFaturamento": 19.88 }
]
```
Erros 400: `limite` fora de 1 a 50, `ordenarPor` diferente de QUANTIDADE ou FATURAMENTO.

### GET /api/dashboard/vendas-por-categoria (RF02)
```json
[
  { "categoria": "Pizza", "quantidade": 610, "faturamento": 15870.00, "participacao": 86.01 },
  { "categoria": "Bebida", "quantidade": 190, "faturamento": 1810.00, "participacao": 9.81 }
]
```

### GET /api/dashboard/pedidos-recentes

Parâmetro extra `limite` (1 a 50, padrão 10).
```json
[
  { "id": "66f1...", "dataHoraEntrada": "2026-09-23T23:41:00Z", "cliente": "Maria", "itens": 3,
    "valorTotal": 123.93, "status": "EM_PREPARACAO", "statusDescricao": "Em preparação", "tipoEntrega": "ENTREGA" }
]
```

### GET /api/dashboard/filtros (RF05)

Opções para montar os filtros da tela.
```json
{
  "status": [ { "valor": "RECEBIDO", "descricao": "Recebido" } ],
  "tiposEntrega": [ { "valor": "ENTREGA", "descricao": "Entrega" }, { "valor": "RETIRADA", "descricao": "Retirada no balcão" } ],
  "categorias": [ "Bebida", "Pizza" ],
  "produtos": [ { "valor": "66f0a1...", "descricao": "Calabresa" } ],
  "agrupamentos": [ "DIA", "SEMANA", "MES" ],
  "periodoMaximoDias": 366
}
```

### Exemplos de erro do dashboard

```
GET /api/dashboard/resumo?de=2026-13-01&status=XPTO&tipoEntrega=DRONE
```
```json
{
  "status": 400,
  "erro": "BAD_REQUEST",
  "mensagem": "Dados inválidos.",
  "detalhes": [
    "Parâmetro 'de' inválido: use o formato AAAA-MM-DD.",
    "Status inválido: 'XPTO'.",
    "tipoEntrega inválido: 'DRONE'. Use ENTREGA ou RETIRADA."
  ],
  "path": "/api/dashboard/resumo"
}
```
Usuário ATENDENTE: `403` com `"mensagem": "Você não tem permissão para executar esta operação."`. Sem login: `401`.

---

# Parte 2: API de pedidos, clientes, produtos, usuários e auditoria

## Pedidos (`/api/pedidos`)

| Método e rota | Perfis | Descrição |
| --- | --- | --- |
| `GET /api/pedidos?status=` | ADMIN, GERENTE, ATENDENTE | Lista pedidos (mais recentes primeiro), com filtro opcional de status. |
| `GET /api/pedidos/{id}` | ADMIN, GERENTE, ATENDENTE | Detalhe do pedido, com itens e histórico. |
| `POST /api/pedidos` | GERENTE, ATENDENTE | Registra pedido. Retorna 201 e o cabeçalho `Location`. |
| `PUT /api/pedidos/{id}/status` | GERENTE, ATENDENTE | Avança o status conforme o fluxo. |
| `POST /api/pedidos/{id}/desconto` | GERENTE | Aplica desconto percentual (0 a 20). |
| `POST /api/pedidos/{id}/cancelar` | GERENTE | Cancela com justificativa. |

**Criar pedido.** Apenas produto e quantidade são aceitos; preço, total, status e responsável são definidos pelo servidor.
```json
POST /api/pedidos
{
  "clienteId": "66f0c1...",
  "tipoEntrega": "ENTREGA",
  "observacao": "sem cebola",
  "itens": [ { "produtoId": "66f0a1...", "quantidade": 2 }, { "produtoId": "66f0a9...", "quantidade": 1 } ]
}
```
Resposta 201 (resumida):
```json
{
  "id": "66f1...", "clienteNome": "Maria", "tipoEntrega": "ENTREGA",
  "itens": [ { "produtoId": "66f0a1...", "nomeProduto": "Calabresa", "categoria": "Pizza", "quantidade": 2, "precoUnitario": 45.90, "subtotal": 91.80 } ],
  "subtotal": 105.80, "percentualDesconto": 0.00, "valorDesconto": 0.00, "valorTotal": 105.80,
  "status": "RECEBIDO", "dataHoraEntrada": "2026-09-23T22:00:00Z", "registradoPor": "atendente01",
  "historico": [ { "statusAnterior": null, "statusNovo": "RECEBIDO", "usuario": "atendente01", "perfil": "ATENDENTE" } ]
}
```
Erros: 400 (sem itens, quantidade fora de 1 a 99, produto inativo ou inexistente), 404 (cliente inexistente), 422 (entrega para cliente sem endereço).

**Avançar status.**
```json
PUT /api/pedidos/{id}/status
{ "status": "SAIU_PARA_ENTREGA", "observacao": "Motoboy João" }
```
Também aceita `?status=` por compatibilidade com a versão anterior. Na saída são gravados `dataHoraSaida` e `responsavelSaida`. Erros: 400 (status inválido), 422 (transição fora do fluxo, pedido cancelado ou finalizado, ou tentativa de usar `CANCELADO` por esta rota).

**Desconto.**
```json
POST /api/pedidos/{id}/desconto
{ "percentual": 10, "justificativa": "Cliente fidelidade" }
```
Erros: 400 (percentual negativo, acima de 20 ou com mais de duas casas, justificativa com menos de 5 caracteres), 403 (perfil diferente de GERENTE), 422 (pedido já saiu, está cancelado ou é registro antigo sem itens).

**Cancelamento.**
```json
POST /api/pedidos/{id}/cancelar
{ "justificativa": "Cliente desistiu por telefone" }
```
Erros: 400 (justificativa com menos de 10 caracteres), 403, 422 (pedido já saiu ou já está cancelado).

## Clientes (`/api/clientes`)

| Método e rota | Perfis | Descrição |
| --- | --- | --- |
| `GET /api/clientes` e `GET /api/clientes/{id}` | GERENTE, ATENDENTE | Consulta. |
| `POST /api/clientes` | ATENDENTE | Cadastro (201). |
| `PUT /api/clientes/{id}` | ATENDENTE | Alteração auditada campo a campo. |
| `DELETE /api/clientes/{id}` | GERENTE | Exclusão (204), recusada com 422 se houver pedidos. |

```json
{ "nome": "Maria", "telefone": "(11) 98765-4321", "cep": "08710-000", "logradouro": "Av. Brasil",
  "numero": "10", "complemento": null, "bairro": "Centro", "cidade": "Mogi das Cruzes", "estado": "SP" }
```
Validações (400): nome obrigatório, telefone com 10 ou 11 dígitos, CEP no formato 00000-000 e estado com 2 letras.

## Produtos (`/api/produtos`)

`GET` para qualquer perfil; `POST`, `PUT /{id}` e `DELETE /{id}` apenas GERENTE (auditados).
```json
{ "nome": "Calabresa", "tipo": "Pizza", "preco": 49.90, "ativo": true }
```

## Usuários (`/api/usuarios`), somente ADMIN

| Método e rota | Descrição |
| --- | --- |
| `GET /api/usuarios`, `GET /api/usuarios/{id}` | Consulta (sem senha). |
| `POST /api/usuarios` | Cria usuário (201). |
| `PUT /api/usuarios/{id}` | Altera nome, perfil e, opcionalmente, senha. |
| `DELETE /api/usuarios/{id}` | Exclui (204). |

```json
POST /api/usuarios
{ "username": "gerente02", "password": "segredo123", "perfil": "GERENTE" }
```
Resposta: `{ "id": "...", "username": "gerente02", "perfil": "GERENTE" }`.
Erros: 400 (perfil inexistente, senha com menos de 6 caracteres, nome inválido), 422 (nome duplicado, alterar o próprio perfil, excluir a própria conta, remover o último ADMIN).

## Auditoria (`/api/auditoria`), ADMIN e GERENTE

| Método e rota | Descrição |
| --- | --- |
| `GET /api/auditoria` | Consulta paginada. Filtros: `entidade` (PEDIDO, CLIENTE, PRODUTO, USUARIO, SISTEMA), `entidadeId`, `usuario` (prefixo), `operacao`, `de`, `ate`, `page` (padrão 0), `size` (1 a 100, padrão 20). |
| `GET /api/auditoria/{id}` | Um registro. |
| `GET /api/auditoria/integridade` | Verifica o encadeamento de hashes. |

Não existem `POST`, `PUT` nem `DELETE` (respondem 405).

```
GET /api/auditoria?entidade=CLIENTE&operacao=ALTERACAO_CLIENTE&size=1
```
```json
{
  "conteudo": [ {
    "id": "66f2...", "sequencia": 41, "dataHora": "2026-09-23T17:35:00.123Z",
    "usuario": "atendente01", "perfil": "ATENDENTE",
    "operacao": "ALTERACAO_CLIENTE", "entidade": "CLIENTE", "entidadeId": "66f0c1...",
    "alteracoes": [ { "campo": "logradouro", "valorAnterior": "Rua das Flores", "valorNovo": "Av. Brasil" },
                    { "campo": "telefone", "valorAnterior": "*******4321", "valorNovo": "*******0000" } ],
    "justificativa": null,
    "hashAnterior": "9f2c...", "hash": "a71b..."
  } ],
  "pagina": 0, "tamanho": 1, "totalElementos": 7, "totalPaginas": 7
}
```
```json
GET /api/auditoria/integridade
{ "integro": true, "registrosVerificados": 128, "primeiraSequenciaInvalida": null, "mensagem": "Todos os 128 registros estão íntegros." }
```

## Testando a API pelo terminal (curl)

```bash
# 1. login (guarda o cookie de sessão)
curl -c cookies.txt -b cookies.txt http://localhost:8080/login -o login.html
TOKEN=$(grep -o 'name="_csrf" value="[^"]*"' login.html | sed 's/.*value="//;s/"//')
curl -c cookies.txt -b cookies.txt -d "username=gerente01&password=gerente123&_csrf=$TOKEN" http://localhost:8080/login

# 2. consultas do dashboard
curl -b cookies.txt "http://localhost:8080/api/dashboard/resumo?de=2026-09-01&ate=2026-09-30"
curl -b cookies.txt "http://localhost:8080/api/dashboard/produtos-mais-vendidos?limite=5&ordenarPor=FATURAMENTO"
```
