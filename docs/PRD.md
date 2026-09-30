# PRD — Balcão PDV

**Versão:** 1.0 · **Data:** 29/09/2026
**Stack:** Java + Spring Boot + Spring Data JPA/Hibernate + PostgreSQL · API REST
**Documento relacionado:** [BRIEFING.md](./BRIEFING.md)

---

## 1. Visão geral

Um PDV para uma loja única com um caixa, capaz de registrar as vendas do começo ao fim: do produto passado no leitor à conferência do caixa no fim do dia.

## 2. Estado atual (baseline já entregue)

> Lista completa, item por item, conforme o status enviado pelo cliente. Estes requisitos continuam valendo: nada das fases novas pode quebrar o que já existe.

### Fase 1 — Produtos ✅

| ID | Requisito | Status |
|---|---|---|
| RF-PRO-01 | Cadastro de produtos | ✅ |
| RF-PRO-02 | Listagem de produtos ativos | ✅ |
| RF-PRO-03 | Busca por ID | ✅ |
| RF-PRO-04 | Busca por código interno | ✅ |
| RF-PRO-05 | Busca por GTIN/código de barras | ✅ |
| RF-PRO-06 | Busca parcial por nome | ✅ |
| RF-PRO-07 | Atualização de produtos | ✅ |
| RF-PRO-08 | Validação de nome e preço | ✅ |
| RF-PRO-09 | Bloqueio de GTIN duplicado | ✅ |
| RF-PRO-10 | Bloqueio de código interno duplicado | ✅ |
| RF-PRO-11 | Desativação de produto | ✅ |
| RF-PRO-12 | Reativação de produto | ✅ |
| RF-PRO-13 | Contagem de produtos ativos | ✅ |

### Fase 2 — Caixa ✅

| ID | Requisito | Status |
|---|---|---|
| RF-CXB-01 | Abertura de caixa | ✅ |
| RF-CXB-02 | Saldo inicial | ✅ |
| RF-CXB-03 | Bloqueio de dois caixas abertos simultaneamente | ✅ |
| RF-CXB-04 | Consulta do caixa aberto | ✅ |
| RF-CXB-05 | Fechamento de caixa | ✅ |
| RF-CXB-06 | Registro de data/hora de abertura | ✅ |
| RF-CXB-07 | Registro de data/hora de fechamento | ✅ |
| RF-CXB-08 | Suprimento de caixa | ✅ |
| RF-CXB-09 | Sangria de caixa | ✅ |
| RF-CXB-10 | Histórico de movimentações | ✅ |
| RF-CXB-11 | Cálculo do saldo esperado | ✅ |

### Fase 2 — Venda / Carrinho ✅

| ID | Requisito | Status |
|---|---|---|
| RF-CAR-01 | Iniciar uma venda | ✅ |
| RF-CAR-02 | Exigir caixa aberto para iniciar venda | ✅ |
| RF-CAR-03 | Vincular a venda ao caixa | ✅ |
| RF-CAR-04 | Status da venda: ABERTA / FINALIZADA / CANCELADA | ✅ |
| RF-CAR-05 | Adicionar produtos à venda | ✅ |
| RF-CAR-06 | Utilizar automaticamente o preço cadastrado | ✅ |
| RF-CAR-07 | Registrar o preço praticado no momento da venda | ✅ |
| RF-CAR-08 | Informar quantidade | ✅ |
| RF-CAR-09 | Calcular subtotal do item | ✅ |
| RF-CAR-10 | Calcular total da venda | ✅ |
| RF-CAR-11 | Listar os itens da venda | ✅ |
| RF-CAR-12 | Alterar quantidade de item já lançado | ✅ |
| RF-CAR-13 | Recalcular subtotal após alteração | ✅ |
| RF-CAR-14 | Recalcular total da venda | ✅ |
| RF-CAR-15 | Impedir quantidade igual ou menor que zero | ✅ |
| RF-CAR-16 | Remover item da venda | ✅ |
| RF-CAR-17 | Recalcular total após remoção | ✅ |

### Próximas etapas (pedido do cliente → onde está especificado)

Todos os 14 itens pedidos são **obrigatórios (P0)** e estão detalhados nas seções abaixo.

| # | Item pedido | Requisitos | Fase |
|---|---|---|---|
| 1 | Pagamento em dinheiro | RF-PAG-01, RF-PAG-06 | 3 |
| 2 | Pagamento via PIX | RF-PAG-02 | 3 |
| 3 | Pagamento em cartão | RF-PAG-03 | 3 |
| 4 | Validação dos pagamentos | RN-PAG-01 a RN-PAG-06 | 3 |
| 5 | Troco | RF-PAG-06, RN-PAG-04 | 3 |
| 6 | Finalização da venda | RF-VEN-01 a RF-VEN-03, RN-VEN-01 a RN-VEN-07 | 4 |
| 7 | Cancelamento da venda | RF-VEN-04, RN-VEN-05 | 4 |
| 8 | Integração das vendas em dinheiro com o saldo do caixa | RF-VEN-03, RN-CX-02 | 4 |
| 9 | Controle e movimentação de estoque | RF-EST-01 a RF-EST-05, RF-EST-07 | 5 |
| 10 | Fechamento completo e conferência do caixa | RF-CX-01, RF-CX-02, RN-CX-01 a RN-CX-05 | 6 |
| 11 | Extrato/resumo de fechamento | RF-CX-03 a RF-CX-05 | 6 |
| 12 | Histórico de vendas | RF-HIS-01 a RF-HIS-03 | 6 |
| 13 | Melhorias nas respostas da API com DTOs | RF-API-01 a RF-API-06 | 6.5 |
| 14 | Frontend/tela do PDV | RF-UI-01 a RF-UI-09 | 7 |

> O restante deste documento especifica **o que falta**. Itens marcados como P2 são **extras sugeridos**, que não constavam na lista do cliente.

---

## 3. Personas

- **Operador de caixa:** passa os produtos, recebe e finaliza. Prioriza velocidade e uso pelo teclado.
- **Gerente / dono:** cadastra produtos, ajusta o estoque, confere o caixa e consulta vendas.

> No MVP não há login nem perfis (ver a questão Q1).

---

## 4. Requisitos funcionais

Prioridade: **P0** = obrigatório para o MVP · **P1** = importante · **P2** = desejável.

### Fase 3 — Pagamentos

| ID | Requisito | Prioridade |
|---|---|---|
| RF-PAG-01 | Registrar pagamento em **dinheiro** numa venda `ABERTA`, informando o valor recebido. | P0 |
| RF-PAG-02 | Registrar pagamento via **PIX**, informando o valor. | P0 |
| RF-PAG-03 | Registrar pagamento em **cartão**, informando o valor e o tipo (`DEBITO` / `CREDITO`). | P0 |
| RF-PAG-04 | Aceitar **várias formas de pagamento** na mesma venda (pagamento dividido). | P0 |
| RF-PAG-05 | Calcular e exibir o **valor restante** a pagar (total − pagos). | P0 |
| RF-PAG-06 | Calcular o **troco** quando o dinheiro recebido passar do restante. | P0 |
| RF-PAG-07 | Remover um pagamento lançado enquanto a venda estiver `ABERTA`. | P0 |
| RF-PAG-08 | Listar os pagamentos da venda. | P0 |
| RF-PAG-09 | Campo opcional para o NSU/autorização do cartão ou o identificador (E2E ID) do PIX. | P2 |

**Regras de negócio**
- **RN-PAG-01:** o valor do pagamento deve ser > 0.
- **RN-PAG-02:** só é possível lançar ou remover pagamentos em venda `ABERTA`.
- **RN-PAG-03:** PIX e cartão **não podem passar** do valor restante. Só o dinheiro pode passar, e a diferença vira troco.
- **RN-PAG-04:** o troco é calculado **apenas sobre o dinheiro**: `troco = max(0, soma_pagos − total)`, limitado ao total recebido em dinheiro.
- **RN-PAG-05:** não é possível lançar pagamento em venda sem itens ou com total = 0.
- **RN-PAG-06:** se um item for alterado ou removido depois de haver pagamentos e a soma paga passar do novo total, o sistema deve **recalcular o troco** (quando só dinheiro excede) ou **bloquear a alteração** com uma mensagem para remover o pagamento excedente (quando PIX ou cartão excedem).

### Fase 4 — Finalização e cancelamento da venda

| ID | Requisito | Prioridade |
|---|---|---|
| RF-VEN-01 | **Finalizar** a venda, mudando o status para `FINALIZADA` e registrando a data e hora. | P0 |
| RF-VEN-02 | Ao finalizar, **baixar o estoque** de cada item (ver a Fase 5). | P0 |
| RF-VEN-03 | Ao finalizar, lançar no caixa uma movimentação `VENDA_DINHEIRO` com o **valor líquido em dinheiro** (recebido em dinheiro − troco). | P0 |
| RF-VEN-04 | **Cancelar** uma venda `ABERTA`, mudando o status para `CANCELADA` e registrando a data, a hora e o motivo (opcional). | P0 |
| RF-VEN-05 | **Estornar** uma venda `FINALIZADA` do caixa ainda aberto, devolvendo o estoque e lançando uma movimentação `ESTORNO_VENDA` no caixa. | P2 |
| RF-VEN-06 | Consultar a venda em andamento (`ABERTA`) do caixa atual. | P1 |

**Regras de negócio**
- **RN-VEN-01:** só é possível finalizar se `soma_pagos ≥ total` e a venda tiver pelo menos 1 item.
- **RN-VEN-02:** só é possível finalizar se o caixa vinculado estiver **aberto**.
- **RN-VEN-03:** a venda `FINALIZADA` ou `CANCELADA` é **imutável**: não aceita mais itens, pagamentos nem alterações.
- **RN-VEN-04:** a finalização (status, estoque e movimentação de caixa) é **atômica**, numa única transação. Se qualquer passo falhar, nada é gravado.
- **RN-VEN-05:** o cancelamento de venda `ABERTA` não gera movimentação de estoque nem de caixa.
- **RN-VEN-06:** transições de status válidas: `ABERTA → FINALIZADA`, `ABERTA → CANCELADA` e, com o RF-VEN-05, `FINALIZADA → ESTORNADA`. Qualquer outra transição é rejeitada.
- **RN-VEN-07:** proteger a finalização contra **duplo clique ou requisição repetida** (lock otimista com `@Version` na venda, ou verificação de status dentro da transação).

### Fase 5 — Estoque

| ID | Requisito | Prioridade |
|---|---|---|
| RF-EST-01 | Cada produto passa a ter o campo `estoqueAtual` (decimal, para aceitar itens vendidos por peso ou unidade fracionada no futuro). | P0 |
| RF-EST-02 | Registrar cada alteração numa tabela de **movimentações de estoque** (tipo, quantidade, saldo anterior e posterior, data e hora, referência à venda quando houver, observação). | P0 |
| RF-EST-03 | **Entrada** manual de estoque (compra ou reposição). | P0 |
| RF-EST-04 | **Ajuste** manual (inventário ou perda), com observação obrigatória. | P0 |
| RF-EST-05 | **Saída automática** na finalização da venda (`SAIDA_VENDA`). | P0 |
| RF-EST-06 | **Devolução automática** no estorno da venda (`ESTORNO_VENDA`). | P2 |
| RF-EST-07 | Consultar o histórico de movimentações de um produto. | P0 |
| RF-EST-08 | Estoque mínimo por produto e listagem dos produtos abaixo do mínimo. | P2 |

**Regras de negócio**
- **RN-EST-01:** `estoqueAtual` só muda por meio de uma movimentação. Nunca por update direto no cadastro do produto.
- **RN-EST-02:** para qualquer produto, o saldo atual é igual à soma das movimentações (invariante que deve ter teste).
- **RN-EST-03:** comportamento com estoque insuficiente na venda: **configurável**, com o padrão **permitir e avisar** (ver a questão Q3).
- **RN-EST-04:** quantidade de entrada > 0. O ajuste pode ser positivo ou negativo, mas não zero.

### Fase 6 — Fechamento de caixa, conferência e histórico

| ID | Requisito | Prioridade |
|---|---|---|
| RF-CX-01 | No fechamento, o operador informa o **valor contado em dinheiro**. | P0 |
| RF-CX-02 | Calcular a **diferença** (contado − esperado) e classificar o resultado como `CONFERE`, `SOBRA` ou `FALTA`. | P0 |
| RF-CX-03 | Gerar o **extrato de fechamento**: saldo inicial, suprimentos, sangrias, vendas em dinheiro, estornos, saldo esperado, contado e diferença. | P0 |
| RF-CX-04 | O extrato mostra também os **totais por forma de pagamento** (dinheiro, PIX, débito e crédito), a quantidade de vendas finalizadas e canceladas e o ticket médio. | P0 |
| RF-CX-05 | Consultar o extrato de um caixa já fechado pelo ID. | P0 |
| RF-CX-06 | Listar os caixas por período. | P0 |
| RF-HIS-01 | **Histórico de vendas** com filtros por período, status, caixa e forma de pagamento, paginado. | P0 |
| RF-HIS-02 | Detalhe de uma venda: itens, pagamentos, troco, status e datas. | P0 |
| RF-HIS-03 | Totalizadores do filtro (quantidade e valor total). | P0 |

**Regras de negócio**
- **RN-CX-01:** o caixa **não fecha** se houver venda `ABERTA` vinculada a ele. O sistema retorna um erro com a lista das vendas pendentes, que o operador deve finalizar ou cancelar antes.
- **RN-CX-02:** `saldo_esperado = saldo_inicial + suprimentos + vendas_dinheiro_líquidas − sangrias − estornos_dinheiro`.
- **RN-CX-03:** PIX e cartão **não entram** no saldo físico do caixa, mas aparecem no extrato para conferência com o banco e a maquininha.
- **RN-CX-04:** a sangria não pode deixar o saldo esperado negativo.
- **RN-CX-05:** o caixa fechado é imutável.

### Fase 6.5 — Qualidade da API (transversal)

| ID | Requisito | Prioridade |
|---|---|---|
| RF-API-01 | **DTOs de request e response** para todos os endpoints. Nenhuma entidade JPA é exposta direto. | P0 |
| RF-API-02 | Validação com Bean Validation (`@Valid`, `@NotNull`, `@Positive`, `@DecimalMin`…). | P0 |
| RF-API-03 | Padrão único de erro com **Problem Details (RFC 9457)** via `@RestControllerAdvice`, com código de erro de negócio (ex.: `VENDA_NAO_ABERTA`, `PAGAMENTO_EXCEDE_RESTANTE`, `CAIXA_COM_VENDA_ABERTA`). | P0 |
| RF-API-04 | Status HTTP coerentes: 201 na criação, 400 em erro de validação, 404 quando o recurso não existe, 409 em conflito de regra ou estado e 422 em violação de regra de negócio. | P0 |
| RF-API-05 | Paginação (`Pageable`) nas listagens. | P0 |
| RF-API-06 | Documentação OpenAPI/Swagger (springdoc-openapi). | P0 |

### Fase 7 — Frontend / tela do PDV

| ID | Requisito | Prioridade |
|---|---|---|
| RF-UI-01 | **Tela de venda:** campo de busca sempre focado, que aceita o leitor de código de barras (GTIN), o código interno ou o nome. | P0 |
| RF-UI-02 | Lista de itens com quantidade editável, subtotal e remoção. Total em destaque. | P0 |
| RF-UI-03 | Atalho de quantidade antes do código (ex.: `3*7891234567890`). | P0 |
| RF-UI-04 | **Tela de pagamento:** botões por forma de pagamento, valor restante, troco em destaque e botão de finalizar. | P0 |
| RF-UI-05 | **Atalhos de teclado:** F2 buscar produto · F4 pagamento · F8 cancelar venda · F10 finalizar · Esc voltar. | P0 |
| RF-UI-06 | Tela de caixa: abrir, suprimento, sangria e fechar (com o valor contado e o extrato). | P0 |
| RF-UI-07 | Telas de produtos (CRUD) e de estoque (entrada, ajuste e histórico). | P0 |
| RF-UI-08 | Tela de histórico de vendas com filtros e detalhe. | P0 |
| RF-UI-09 | Indicador fixo no topo mostrando se o caixa está aberto ou fechado. | P0 |
| RF-UI-10 | Impressão de um comprovante simples não fiscal (80 mm). | P2 |
| RF-UI-11 | Após finalizar, exibir o **DANFE NFC-e** (cupom com QR Code) pronto para imprimir. | P0 |
| RF-UI-12 | Tela fiscal: dados do emitente, ambiente, série/numeração, CSC e lista de notas emitidas (consulta, XML, cancelamento). | P0 |

### Fase 8 — NFC-e (Nota Fiscal de Consumidor Eletrônica, modelo 65)

| ID | Requisito | Prioridade |
|---|---|---|
| RF-NFC-01 | **Configuração do emitente:** CNPJ, IE, razão social, nome fantasia, endereço completo (com código IBGE do município e UF), CRT (regime tributário). | P0 |
| RF-NFC-02 | **Configuração da emissão:** ambiente (`HOMOLOGACAO` / `PRODUCAO`), série, próximo número, ID do token (CSC ID) e CSC, URLs de QR Code e de consulta da UF, emissão automática ao finalizar (sim/não). | P0 |
| RF-NFC-03 | **Dados fiscais do produto:** NCM (8 dígitos), CFOP (padrão 5102), unidade comercial, origem da mercadoria e CSOSN (Simples) ou CST (Regime Normal). | P0 |
| RF-NFC-04 | **Emitir NFC-e** de uma venda `FINALIZADA`, automaticamente na finalização (se configurado) ou manualmente depois. | P0 |
| RF-NFC-05 | Gerar a **chave de acesso** de 44 dígitos com dígito verificador (módulo 11). | P0 |
| RF-NFC-06 | Gerar o **XML** no layout 4.00 (ide, emit, dest opcional, det/prod/imposto, total, pag com `tPag`/`vTroco`, infAdic, infNFeSupl com QR Code). | P0 |
| RF-NFC-07 | Gerar o **QR Code** versão 2 (hash SHA-1 com o CSC) e a URL de consulta pela chave. | P0 |
| RF-NFC-08 | **CPF/CNPJ do consumidor** opcional na venda ("CPF na nota?"). | P0 |
| RF-NFC-09 | Guardar a nota: número, série, chave, ambiente, status, protocolo, data/hora de emissão e de autorização, XML e motivo de rejeição. | P0 |
| RF-NFC-10 | **Cancelar** uma NFC-e autorizada dentro do prazo legal, com justificativa (15 a 255 caracteres). | P0 |
| RF-NFC-11 | **Reemitir** uma NFC-e rejeitada depois de corrigir os dados (mantém a venda, gera nova tentativa). | P0 |
| RF-NFC-12 | Listar e filtrar as notas (período, status) e baixar o XML. | P0 |
| RF-NFC-13 | Imprimir o **DANFE NFC-e** (cupom 80 mm com itens, totais, pagamentos, troco, chave, protocolo e QR Code). | P0 |
| RF-NFC-14 | **Emissor plugável:** a transmissão fica atrás de uma interface (`EmissorNfce`). O sistema vem com o emissor `SIMULADO` (autoriza localmente, para desenvolvimento e treinamento) e o ponto de encaixe para o emissor real (SEFAZ direto ou via provedor). | P0 |
| RF-NFC-15 | **Contingência offline** (`tpEmis = 9`) quando a SEFAZ estiver fora, com transmissão posterior. | P1 |
| RF-NFC-16 | **Inutilização** de faixas de numeração não usadas. | P1 |
| RF-NFC-17 | **Assinatura digital** do XML (XMLDSig) com certificado **A1** (.pfx) e transmissão via webservice SOAP da SEFAZ da UF. | P1 (exige certificado real) |

**Regras de negócio**
- **RN-NFC-01:** só vendas `FINALIZADA` podem ter NFC-e. Uma venda tem no máximo **uma** NFC-e autorizada.
- **RN-NFC-02:** a numeração é **sequencial por série, sem buracos**. O próximo número é reservado com lock pessimista na configuração fiscal.
- **RN-NFC-03:** a emissão só acontece se a configuração fiscal estiver completa (CNPJ, IE, endereço, CSC). Senão: 422 `CONFIGURACAO_FISCAL_INCOMPLETA`.
- **RN-NFC-04:** todo produto vendido precisa de NCM válido (8 dígitos). Senão: 422 `PRODUTO_SEM_DADOS_FISCAIS`, listando os produtos.
- **RN-NFC-05:** formas de pagamento no XML: dinheiro = `01`, crédito = `03`, débito = `04`, PIX = `17`. O troco vai em `vTroco`.
- **RN-NFC-06:** em homologação, a primeira linha do item deve ser `NOTA FISCAL EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL` e o DANFE traz o aviso.
- **RN-NFC-07:** o cancelamento da NFC-e respeita o prazo configurável (padrão **30 minutos** após a autorização, conferir a regra da UF).
- **RN-NFC-08:** o estorno de uma venda com NFC-e autorizada **exige cancelar a nota antes**.
- **RN-NFC-09:** a falha na emissão **não desfaz a venda**. A venda fica finalizada e a nota fica `REJEITADA`/`PENDENTE` para reemissão.

> **Para emitir em produção** é preciso: credenciamento do CNPJ para NFC-e na SEFAZ do estado, CSC gerado no portal da SEFAZ, certificado digital A1 e a orientação do contador sobre NCM, CFOP e tributação de cada produto. Detalhes em [NFCE.md](./NFCE.md).

---

## 5. Modelo de dados (novo e alterado)

```
Produto (existente)
  + estoqueAtual        NUMERIC(12,3) NOT NULL DEFAULT 0
  + estoqueMinimo       NUMERIC(12,3) NULL                (P2)

Venda (existente)
  + valorPago           NUMERIC(12,2)
  + troco               NUMERIC(12,2)
  + dataFinalizacao     TIMESTAMP
  + dataCancelamento    TIMESTAMP
  + motivoCancelamento  VARCHAR(255)
  + version             BIGINT  (lock otimista)

Pagamento (novo)
  id, venda_id (FK), forma ENUM(DINHEIRO, PIX, CARTAO_DEBITO, CARTAO_CREDITO),
  valor NUMERIC(12,2), identificadorTransacao VARCHAR NULL, dataHora

MovimentacaoEstoque (novo)
  id, produto_id (FK), tipo ENUM(ENTRADA, AJUSTE, SAIDA_VENDA, ESTORNO_VENDA),
  quantidade NUMERIC(12,3), saldoAnterior, saldoPosterior,
  venda_id (FK NULL), observacao, dataHora

MovimentacaoCaixa (existente)
  tipo ENUM += VENDA_DINHEIRO, ESTORNO_VENDA
  + venda_id (FK NULL)

Caixa (existente)
  + valorContado        NUMERIC(12,2)
  + diferenca           NUMERIC(12,2)
  + situacaoConferencia ENUM(CONFERE, SOBRA, FALTA)
```

**Convenções:** dinheiro em `BigDecimal` com escala 2 e `RoundingMode.HALF_EVEN`. Datas em `OffsetDateTime`/`timestamptz`. Migrações versionadas com **Flyway**, recomendado caso ainda use `ddl-auto`.

---

## 6. Endpoints propostos

> Ajuste os prefixos ao padrão que já existe no projeto.

**Pagamentos**
- `POST   /vendas/{id}/pagamentos`: lança um pagamento `{forma, valor, identificadorTransacao?}`
- `GET    /vendas/{id}/pagamentos`: lista os pagamentos, o restante e o troco
- `DELETE /vendas/{id}/pagamentos/{pagamentoId}`: remove um pagamento

**Venda**
- `POST /vendas/{id}/finalizar`
- `POST /vendas/{id}/cancelar`: `{motivo?}`
- `POST /vendas/{id}/estornar`: P2
- `GET  /vendas/aberta`: venda em andamento no caixa atual
- `GET  /vendas?inicio=&fim=&status=&caixaId=&forma=&page=&size=`: histórico
- `GET  /vendas/{id}`: detalhe

**Estoque**
- `POST /produtos/{id}/estoque/entradas`: `{quantidade, observacao?}`
- `POST /produtos/{id}/estoque/ajustes`: `{quantidade (±), observacao}`
- `GET  /produtos/{id}/estoque/movimentacoes`
- `GET  /produtos/estoque-baixo`: P2

**Caixa**
- `POST /caixas/{id}/fechar`: `{valorContado}` → retorna o extrato
- `GET  /caixas/{id}/extrato`
- `GET  /caixas?inicio=&fim=`

---

## 7. Requisitos não funcionais

| ID | Requisito |
|---|---|
| RNF-01 | Adicionar um item por código de barras responde em **< 300 ms** (p95) numa base de 10 mil produtos. Criar índices em `gtin`, `codigo_interno` e `lower(nome)`. |
| RNF-02 | Toda operação que mexe em venda, estoque e caixa roda em **transação** (`@Transactional`). |
| RNF-03 | Nenhum `double`/`float` em valores monetários. |
| RNF-04 | **Testes:** unitários nos services com regra de negócio (troco, restante, saldo esperado, transições de status) e testes de integração com PostgreSQL via Testcontainers para os fluxos de finalizar venda e fechar caixa. |
| RNF-05 | Logs estruturados nas operações críticas: abertura e fechamento de caixa, finalização, cancelamento e estorno. |
| RNF-06 | Backups diários do PostgreSQL no ambiente de produção. |
| RNF-07 | A tela do PDV funciona em resolução 1366×768 e é 100% operável por teclado. |

---

## 8. Fluxos principais

**Venda completa (caminho feliz)**
1. Caixa aberto → `POST /vendas` → venda `ABERTA`.
2. O leitor passa os itens → `POST /vendas/{id}/itens` (já existe).
3. F4 → o operador lança R$ 30 em PIX → restante R$ 17,50.
4. O operador lança R$ 20 em dinheiro → troco R$ 2,50.
5. F10 → `POST /vendas/{id}/finalizar` → status `FINALIZADA`, estoque baixado e `VENDA_DINHEIRO` de R$ 17,50 lançada no caixa.

**Fechamento do caixa**
1. `POST /caixas/{id}/fechar` com `valorContado`.
2. Se houver venda `ABERTA`, o sistema retorna 409 `CAIXA_COM_VENDA_ABERTA` com a lista das vendas.
3. Senão, calcula o esperado e a diferença, grava, fecha e retorna o extrato.

---

## 9. Critérios de aceite (exemplos em Gherkin)

```gherkin
Cenário: Troco em pagamento dividido
  Dado uma venda ABERTA com total de R$ 47,50
  E um pagamento PIX de R$ 30,00
  Quando lanço R$ 20,00 em dinheiro
  Então o restante é R$ 0,00
  E o troco é R$ 2,50

Cenário: PIX excedendo o restante
  Dado uma venda ABERTA com restante de R$ 10,00
  Quando lanço R$ 15,00 em PIX
  Então recebo erro 422 PAGAMENTO_EXCEDE_RESTANTE

Cenário: Finalizar sem pagamento suficiente
  Dado uma venda ABERTA com total R$ 50,00 e pagos R$ 40,00
  Quando tento finalizar
  Então recebo erro 422 PAGAMENTO_INSUFICIENTE
  E a venda continua ABERTA

Cenário: Finalização lança dinheiro líquido no caixa
  Dado um caixa com saldo esperado de R$ 100,00
  E uma venda de R$ 47,50 paga com R$ 50,00 em dinheiro
  Quando finalizo a venda
  Então o saldo esperado do caixa passa a R$ 147,50
  E o estoque de cada item é reduzido pela quantidade vendida

Cenário: Fechar caixa com venda aberta
  Dado um caixa aberto com uma venda ABERTA
  Quando tento fechar o caixa
  Então recebo erro 409 CAIXA_COM_VENDA_ABERTA

Cenário: Conferência com falta
  Dado um caixa com saldo esperado de R$ 500,00
  Quando fecho informando R$ 480,00 contados
  Então a diferença é -R$ 20,00 e a situação é FALTA

Cenário: Venda finalizada é imutável
  Dado uma venda FINALIZADA
  Quando tento adicionar um item ou pagamento
  Então recebo erro 409 VENDA_NAO_ABERTA
```

---

## 10. Questões em aberto (validar com o cliente)

| # | Pergunta | Sugestão padrão |
|---|---|---|
| Q1 | Vai haver mais de um operador? Precisa de login e registro de quem fez cada venda? | MVP sem login. Login simples na fase seguinte. |
| Q2 | O PIX e o cartão serão só registrados ou integrados (QR dinâmico, TEF)? | Só registro no MVP. |
| Q3 | Com estoque zerado, o sistema bloqueia a venda ou apenas avisa? | Avisar e permitir, com a opção de configurar. |
| Q4 | A loja vende por peso (balança) ou fração? | O modelo já aceita decimal. A UI trata depois. |
| Q5 | Qual o regime tributário (Simples ou Normal), a UF e quem será o provedor de transmissão da NFC-e? | NFC-e incluída (Fase 8). Simples Nacional (CSOSN 102) como padrão. Validar com o contador. |
| Q6 | Descontos por item ou no total da venda são necessários? | Desconto manual no total como P1, se pedido. |
| Q7 | Precisa imprimir comprovante? Qual impressora? | Comprovante não fiscal 80 mm como P2. |
| Q8 | O estorno de venda finalizada é necessário no MVP? | P2. |

---

## 11. Roadmap sugerido

| Fase | Entrega | Depende de |
|---|---|---|
| **3** | Pagamentos (dinheiro, PIX, cartão, troco, validação) | — |
| **4** | Finalização e cancelamento da venda + integração com o caixa | 3 |
| **5** | Estoque (campo, movimentações, entrada, ajuste, baixa na venda) | — (em paralelo com a 3) |
| **6** | Fechamento com conferência, extrato e histórico de vendas | 4, 5 |
| **6.5** | DTOs, erros padronizados, paginação e OpenAPI | transversal: começar já e aplicar em cada fase |
| **7** | Frontend do PDV | 4 (tela de venda), 6 (tela de caixa) |
| **8** | NFC-e (config, chave, XML, QR Code, DANFE, cancelamento, emissor simulado + porta para o real) | 4 |

**Definição de pronto (por fase):** regras cobertas por testes, endpoints com DTOs e erros padronizados, documentação OpenAPI atualizada e código revisado e mergeado na `main`.

---

## 12. Análise de mercado: o que faltava para competir

A lista original cobre o ciclo básico da venda. Comparando com os PDVs mais usados no varejo pequeno brasileiro (mercadinhos, conveniências, padarias, lojas de bairro), estes pontos aparecem em praticamente todos. Quem não tem, perde a venda para o concorrente.

| # | Recurso | Por que importa | Situação |
|---|---|---|---|
| M1 | **Operadores com login por PIN e perfis** (operador, gerente, admin) | Saber quem vendeu, quem cancelou, quem mexeu no caixa | ✅ Fase 9 |
| M2 | **Autorização do gerente** (PIN do supervisor) para cancelar venda, estornar, dar desconto acima do limite e fazer sangria | Controle antifraude básico, cobrado por todo dono de loja | ✅ Fase 9 |
| M3 | **Cadastro da loja e personalização** (nome, logo, cor, mensagem do cupom) | O sistema tem a cara da loja, no PDV e no cupom | ✅ Fase 9 |
| M4 | **Painel de vendas (dashboard)**: faturamento do dia, ticket médio, lucro bruto, vendas por hora, formas de pagamento, mais vendidos, comparação com ontem | O dono acompanha o dia sem abrir relatório | ✅ Fase 10 |
| M5 | **Descontos** no total (valor ou %) com limite por perfil | Negociação no balcão, com rateio correto na NFC-e | ✅ Fase 9 |
| M6 | **Clientes e fiado (crediário)** com limite de crédito, conta corrente e recebimento | Muito usado em bairro. Sem isso, o dono volta pro caderninho | ✅ Fase 11 |
| M7 | **Vale-alimentação e vale-refeição** como formas de pagamento | Obrigatório em mercado e padaria | ✅ Fase 9 |
| M8 | **Etiqueta de balança** (EAN-13 começando com 2, com preço ou peso embutido) | Todo mercado com açougue, frios ou hortifrúti usa | ✅ Fase 9 |
| M9 | **Preço promocional com vigência** | Ofertas da semana sem mexer no preço normal | ✅ Fase 9 |
| M10 | **Preço de custo e margem** por produto | Saber quanto lucra, não só quanto vende | ✅ Fase 9 |
| M11 | **Categorias** de produto | Organizar cadastro e relatórios | ✅ Fase 9 |
| M12 | **Curva ABC** de produtos | Saber o que não pode faltar na prateleira | ✅ Fase 10 |
| M13 | **Alerta de sangria** (limite de dinheiro na gaveta) | Segurança: menos dinheiro exposto | ✅ Fase 9 |
| M15 | Troca e devolução com vale-troca | Comum em loja de roupa e presentes | ✅ |
| M16 | Orçamento / pré-venda (vendedor monta, caixa recebe) | Lojas com balcão e caixa separados | Backlog (P1) |
| M17 | PIX com QR Code dinâmico (integração com banco/PSP) e baixa automática | Elimina conferência manual do PIX | Backlog (P1) |
| M18 | TEF (maquininha integrada) | Elimina digitação do valor na maquininha | Backlog (P2) |
| M19 | Entrada de mercadoria pelo XML da NF-e do fornecedor | Cadastra e dá entrada no estoque de uma vez | Backlog (P1) |
| M20 | Impressão de etiquetas de gôndola | Preço na prateleira sempre igual ao do sistema | Backlog (P2) |
| M21 | Modo offline (vender sem internet e sincronizar) + contingência NFC-e | Internet cai; a venda não pode parar | Backlog (P1) |
| M22 | Tributos aproximados (Lei 12.741/IBPT) no cupom | Exigência legal no cupom | Backlog (P1) |
| M23 | Multi-loja / multi-caixa | Crescimento do cliente | Backlog (P2) |
| M24 | Integração com delivery (iFood etc.) e loja virtual | Canais extras | Backlog (P2) |

### Fase 9 — Loja, operadores e regras da venda

| ID | Requisito |
|---|---|
| RF-LOJ-01 | Cadastro da loja: nome, slogan, logo (PNG/JPG/SVG até 300 KB), cor de destaque, mensagem do rodapé do cupom. |
| RF-LOJ-02 | Regras da venda: política de estoque (avisar ou bloquear), limite de dinheiro na gaveta (alerta de sangria), desconto máximo do operador (%). |
| RF-LOJ-03 | Configuração da etiqueta de balança: prefixo, quantidade de dígitos do código e se o valor embutido é preço ou peso. |
| RF-USU-01 | Usuários com nome, perfil (`OPERADOR`, `GERENTE`, `ADMIN`) e PIN de 4 a 6 dígitos (guardado com BCrypt). |
| RF-USU-02 | Login por seleção do operador e PIN. Sessão por token com validade de 12 h. |
| RF-USU-03 | Primeiro acesso: se não existir usuário, a tela pede a criação do administrador. |
| RF-USU-04 | Ações sensíveis exigem gerente ou autorização pontual com PIN de gerente: cancelar venda, estornar, desconto acima do limite, sangria, cancelar NFC-e. |
| RF-USU-05 | Venda, cancelamento e movimentações de caixa registram o operador. |
| RF-DES-01 | Desconto no total da venda, em valor ou percentual, recalculado quando os itens mudam. Rateado por item no XML da NFC-e (`vDesc`). |
| RF-PAG-10 | Formas de pagamento: vale-alimentação (`tPag 10`), vale-refeição (`tPag 11`) e crediário/fiado (`tPag 05`). |
| RF-PRO-14 | Categoria, preço de custo e margem calculada no cadastro de produto. |
| RF-PRO-15 | Preço promocional com data de início e fim, aplicado automaticamente na venda. |
| RF-PRO-16 | Leitura de etiqueta de balança (EAN-13 com prefixo `2`): identifica o produto pelo código interno e calcula a quantidade pelo peso ou pelo preço embutido. |

### Fase 10 — Painel e relatórios

| ID | Requisito |
|---|---|
| RF-PAI-01 | Painel do dia: faturamento, número de vendas, ticket médio, itens vendidos, lucro bruto estimado e descontos, com comparação com o dia anterior. |
| RF-PAI-02 | Gráfico de vendas por hora, faturamento por forma de pagamento, top 10 produtos, vendas por categoria e por operador. |
| RF-PAI-03 | Alertas: estoque baixo, notas rejeitadas/pendentes, gaveta acima do limite, fiado a receber. |
| RF-REL-01 | Curva ABC por período (A = 80% do faturamento, B = 15%, C = 5%). |

### Fase 11 — Clientes e fiado

| ID | Requisito |
|---|---|
| RF-CLI-01 | Cadastro de cliente: nome, CPF/CNPJ, telefone, e-mail, limite de crédito. |
| RF-CLI-02 | Vincular cliente à venda (preenche o CPF da nota automaticamente). |
| RF-CLI-03 | Pagamento em crediário exige cliente e respeita o limite (`saldo devedor + valor ≤ limite`). |
| RF-CLI-04 | Conta corrente do cliente: compras, pagamentos e estornos, com saldo devedor. |
| RF-CLI-05 | Recebimento de fiado em qualquer forma. Em dinheiro, entra no caixa como `RECEBIMENTO_CLIENTE`. |
