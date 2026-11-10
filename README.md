# PediuPatrão

Este projeto evolui o sistema de pedidos de uma pizzaria, com foco em controle de acesso por perfil, regras de negócio para os pedidos, auditoria das alterações e um dashboard administrativo alimentado por API. A solução foi desenvolvida em duas etapas:

- **Etapa 1** perfis de acesso, controle de entrada e saída dos pedidos, descontos, cancelamentos e auditoria.
- **Etapa 2** dashboard administrativo com indicadores, gráficos e filtros, consumindo uma API REST própria.

Tecnologias: Java 21, Spring Boot 3.4, Spring Security, Spring Data MongoDB e Thymeleaf.


## O que foi implementado

- Três perfis de acesso (ADMIN, GERENTE e ATENDENTE), com autorização verificada no backend;
- Proteção CSRF em formulários e chamadas da API;
- Proteção contra atribuição indevida de privilégios na administração de usuários;
- Pedidos com itens, quantidades, preços, desconto, valor total, responsável e data/hora de entrada;
- Cálculo do valor total feito exclusivamente no servidor;
- Fluxo obrigatório de status, com bloqueio de transições inválidas;
- Registro de data/hora e responsável pela saída do pedido;
- Desconto de até 20% e cancelamento com justificativa, exclusivos do gerente;
- Auditoria persistente no MongoDB, somente inclusão, com verificação de integridade;
- Dashboard com indicadores de vendas, gráficos, ranking de produtos e filtros;
- 78 testes automatizados, executados também no GitHub Actions.

## Estrutura de segurança

### 1. Perfis de acesso

Cada usuário possui um perfil, convertido em role do Spring Security no login:

- **ADMIN:** administra usuários e perfis, consulta pedidos, auditoria e dashboard;
- **GERENTE:** gerencia pedidos, aplica descontos, cancela pedidos, consulta clientes, auditoria e dashboard;
- **ATENDENTE:** cadastra e atualiza clientes, registra pedidos e acompanha o andamento.

Somente o GERENTE aplica descontos e cancela pedidos, inclusive em comparação ao ADMIN.

### 2. Autorização no backend

A proteção não depende da interface. Ela é aplicada em duas camadas:

- regras por URL e método HTTP no `SecurityConfig`;
- anotações `@PreAuthorize` nos serviços, que protegem a regra mesmo que uma nova rota seja criada.

Requisições sem login recebem 401, e tentativas sem permissão recebem 403 e são registradas em log e na auditoria.

### 3. Administração de usuários

A rota de usuários, que antes era pública, passou a ser exclusiva do ADMIN. O perfil informado é validado no servidor, o administrador não pode alterar o próprio perfil nem excluir a própria conta, e o sistema nunca fica sem administrador. As senhas são armazenadas com hash BCrypt e nunca aparecem nas respostas da API.

### 4. Integridade e proteção de dados

- Os registros de auditoria não podem ser alterados nem excluídos pela aplicação ou pela API;
- Cada registro guarda o hash do anterior, permitindo detectar alterações feitas diretamente no banco;
- Senhas e tokens nunca são registrados em logs ou na auditoria;
- Telefones são gravados mascarados na auditoria, mantendo apenas os quatro últimos dígitos.

## Modelo de dados

As coleções principais no MongoDB são:

- `usuarios`: nome de usuário, hash da senha e perfil;
- `clientes`: dados cadastrais, endereço e telefone;
- `produtos`: nome, categoria, preço e situação;
- `pedidos`: cliente, itens, valores, status, entrada, saída, desconto, cancelamento e histórico de status;
- `auditoria`: usuário, perfil, data/hora, operação, entidade, campos alterados, justificativa e hash.

## Fluxo das informações

### Registro do pedido

1. O atendente seleciona o cliente, o tipo (entrega ou retirada) e os itens.
2. O frontend envia `POST /api/pedidos` apenas com produtos e quantidades.
3. A API valida o cliente, os produtos e as quantidades.
4. Os preços são buscados no cadastro de produtos e o total é calculado no servidor.
5. O pedido é salvo com status RECEBIDO, data/hora de entrada e usuário responsável.
6. A inclusão é registrada na auditoria.

### Andamento do pedido

1. O fluxo obrigatório é: RECEBIDO, EM PREPARAÇÃO, PRONTO, SAIU PARA ENTREGA ou RETIRADO, e FINALIZADO.
2. O frontend envia `PUT /api/pedidos/{id}/status` com o novo status.
3. A API verifica se a transição é permitida a partir do status atual.
4. Transições inválidas, como finalizar um pedido em preparação, são recusadas.
5. Na saída, a API registra a data/hora e o responsável.
6. Cada mudança é gravada no histórico do pedido e na auditoria.

### Desconto

1. O gerente informa o percentual e a justificativa.
2. O frontend envia `POST /api/pedidos/{id}/desconto`.
3. A API verifica o perfil, o limite de 20% e se o pedido ainda não saiu.
4. O total é recalculado no servidor.
5. Os valores anteriores e novos são registrados na auditoria.

### Cancelamento

1. O gerente informa a justificativa (mínimo de 10 caracteres).
2. O frontend envia `POST /api/pedidos/{id}/cancelar`.
3. A API verifica o perfil e se o pedido ainda não saiu para entrega ou retirada.
4. O pedido passa a CANCELADO, preservando itens e valores.
5. Nenhuma alteração de status é aceita depois disso.
6. O cancelamento e a justificativa são registrados na auditoria.

### Alteração de cliente

1. O atendente altera os dados do cliente.
2. A API compara os dados anteriores com os novos.
3. Apenas os campos alterados são registrados na auditoria, com valor anterior e novo.

### Dashboard

1. O ADMIN ou GERENTE acessa o dashboard e escolhe os filtros.
2. O frontend chama em paralelo os endpoints de `/api/dashboard`.
3. A API filtra os pedidos no banco e calcula os indicadores.
4. Os gráficos são atualizados automaticamente a cada 60 segundos.
5. Erros e períodos sem dados são exibidos sem interromper a tela.

## Como executar

Pré-requisitos: Java 21 e MongoDB.

```powershell
$env:MONGODB_URI="mongodb://localhost:27017"; $env:MONGODB_DATABASE="pizzaria"; .\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--app.seed.pedidos-demo=true"
```

Acesse `http://localhost:8080`. O parâmetro `app.seed.pedidos-demo=true` gera dados de demonstração para o dashboard na primeira execução.

Usuários de demonstração:

- `admin` / `teste123` (ADMIN)
- `gerente01` / `gerente123` (GERENTE)
- `atendente01` / `atendente123` (ATENDENTE)

Para rodar os testes:

```powershell
.\mvnw.cmd test
```

## Endpoints principais

- `POST /api/pedidos`: registra pedido
- `PUT /api/pedidos/{id}/status`: avança o status do pedido
- `POST /api/pedidos/{id}/desconto`: aplica desconto (GERENTE)
- `POST /api/pedidos/{id}/cancelar`: cancela pedido com justificativa (GERENTE)
- `POST /api/clientes` e `PUT /api/clientes/{id}`: cadastro e alteração de clientes (ATENDENTE)
- `POST /api/usuarios`: cadastro de usuários (ADMIN)
- `GET /api/auditoria`: consulta da auditoria (ADMIN e GERENTE)
- `GET /api/auditoria/integridade`: verificação de integridade da auditoria
- `GET /api/dashboard/resumo`: indicadores de vendas
- `GET /api/dashboard/vendas-por-periodo`: faturamento por dia, semana ou mês
- `GET /api/dashboard/status`: situação dos pedidos
- `GET /api/dashboard/produtos-mais-vendidos`: ranking de produtos
