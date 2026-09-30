# Briefing — Balcão PDV

**Data:** 29/09/2026
**Status:** Fases 1 e 2 (produtos, caixa e carrinho) concluídas no backend. Próxima meta: fechar o ciclo completo de uma venda.

---

## 1. Contexto

Um amigo precisa de um **PDV (Ponto de Venda)** para registrar as vendas do balcão: cadastrar produtos, abrir o caixa, passar os itens, receber em dinheiro, PIX ou cartão, dar troco e, no fim do dia, conferir o caixa.

O backend já existe e cobre o cadastro de produtos, a abertura e o fechamento de caixa e a montagem do carrinho. **Ainda não dá para concluir uma venda.** Faltam pagamento, finalização, estoque, conferência do caixa e a tela de operação.

## 2. Problema

Hoje o sistema monta uma venda, mas não a conclui. Por isso:

- não entra dinheiro no caixa, e o saldo esperado fica errado;
- o estoque não baixa;
- o operador não tem uma tela para trabalhar, só a API.

## 3. Objetivo

Entregar um PDV **utilizável no balcão**, em que o operador:

1. abre o caixa;
2. passa os produtos com o leitor de código de barras;
3. recebe o pagamento, podendo combinar formas (ex.: parte em PIX e parte em dinheiro);
4. finaliza a venda, com baixa de estoque e o dinheiro entrando no saldo do caixa;
5. fecha o caixa no fim do dia e vê um resumo com as diferenças encontradas.

## 4. Público

| Perfil | O que precisa |
|---|---|
| **Operador de caixa** | Rapidez, uso pelo teclado e leitor, poucos cliques, mensagens de erro claras. |
| **Dono / gerente** | Cadastro de produtos, controle de estoque, conferência do caixa e histórico de vendas. |

No MVP é uma única loja, com **um caixa aberto por vez**.

## 5. Escopo

**Já entregue:** todos os 41 itens das Fases 1 e 2 (13 de produtos, 11 de caixa e 17 de venda/carrinho), listados um a um na seção 2 do PRD.

**Entra (próximas entregas, todas obrigatórias):**
1. Pagamento em dinheiro
2. Pagamento via PIX
3. Pagamento em cartão
4. Validação dos pagamentos
5. Troco
6. Finalização da venda
7. Cancelamento da venda
8. Integração das vendas em dinheiro com o saldo do caixa
9. Controle e movimentação de estoque
10. Fechamento completo e conferência do caixa
11. Extrato/resumo de fechamento
12. Histórico de vendas
13. Melhorias nas respostas da API com DTOs
14. Frontend/tela do PDV
15. **NFC-e** (nota fiscal de consumidor eletrônica, modelo 65): configuração do emitente, chave de acesso, XML 4.00, QR Code, DANFE, cancelamento e reemissão

**Não entra (por enquanto):**
- SAT (SP) e NF-e modelo 55;
- integração automática com maquininha (TEF) ou com a API do banco/PSP para o PIX. No MVP o pagamento é **registrado manualmente**;
- vários caixas ou várias lojas;
- cadastro de clientes, fiado ou crediário;
- promoções, descontos por regra e programa de fidelidade;
- relatórios gerenciais avançados.

## 6. Stack

- **Backend:** Java + Spring Boot + Spring Data JPA/Hibernate + PostgreSQL (API REST)
- **Versionamento:** Git/GitHub
- **Frontend:** React + TypeScript + Vite, com a tela focada em teclado e leitor.

## 7. Critérios de sucesso

- Uma venda completa (abrir a venda, passar 5 itens, pagar em 2 formas, finalizar) leva **menos de 30 segundos** para um operador treinado.
- O saldo esperado do caixa bate com a soma de suprimentos, vendas em dinheiro e sangrias, **sem diferença de centavos**.
- O estoque de cada produto é igual à soma do seu histórico de movimentações.
- Nenhuma venda fica finalizada sem pagamento suficiente, e nenhum caixa fecha com venda em aberto.

## 8. Entregas

1. **PRD** (`docs/PRD.md`), com os requisitos detalhados, as regras de negócio e os critérios de aceite.
2. Backend das próximas etapas (Fases 3 a 6 do PRD).
3. Tela do PDV (Fase 7).
4. NFC-e (Fase 8), com o guia de ativação em `docs/NFCE.md`.

## 9. Riscos e pontos de atenção

- **Fiscal:** a NFC-e está no escopo. O sistema vem com um emissor **simulado** (para testar e treinar sem valor fiscal). Para emitir de verdade são necessários credenciamento na SEFAZ, CSC, certificado A1 e a definição de NCM/CFOP/CSOSN pelo contador (ver `docs/NFCE.md`).
- **Dinheiro:** todo valor deve usar `BigDecimal` com 2 casas decimais e um arredondamento definido. `double` nunca.
- **Consistência:** a finalização da venda (pagamentos, estoque e caixa) precisa rodar numa **única transação**.
- **Estoque negativo:** é preciso decidir se o sistema bloqueia a venda ou apenas avisa quando o estoque acaba (ver o PRD, item 10).

## 10. Decisões pendentes com o cliente

Ver a seção "Questões em aberto" do PRD.
