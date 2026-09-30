# Balcão PDV

Ponto de venda para loja de balcão: leitor de código de barras, pagamento dividido com troco, caixa com sangria/suprimento e conferência, estoque com histórico, histórico de vendas e NFC-e.

- **Backend:** Java 21 · Spring Boot 3.4 · Spring Data JPA · PostgreSQL 16 · Flyway · API REST (Problem Details)
- **Frontend:** React 18 · TypeScript · Vite
- **Docs:** [Briefing](docs/BRIEFING.md) · [PRD](docs/PRD.md) · [NFC-e](docs/NFCE.md)

## Rodando

Pré-requisitos: Java 21, Maven, Node 20+ e Docker.

```bash
# 1. Banco
docker compose up -d

# 2. Backend (perfil demo = 16 produtos e emitente fictício em homologação)
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=demo
# API em http://localhost:8080 · Swagger em http://localhost:8080/swagger-ui.html

# 3. Frontend
cd frontend
npm install
npm run dev
# PDV em http://localhost:5173
```

Sem o perfil `demo`, o sistema sobe vazio: cadastre os produtos e o emitente pelas telas.

## Testes

```bash
cd backend
mvn test   # unitários + integração com PostgreSQL real via Testcontainers (precisa do Docker)
```

## Atalhos do PDV

| Tecla | Ação |
|---|---|
| F1 · F6 · F7 · F9 · F11 | Vender · Caixa · Produtos · Vendas · Fiscal |
| F2 | Focar o leitor |
| `3*código` | Lança 3 unidades (`0,350*015` = 350 g) |
| ↑ ↓ · + − · Del | Escolher linha · mudar quantidade · remover |
| F3 | CPF/CNPJ na nota |
| F4 | Receber |
| D · P · B · C | Dinheiro · PIX · Débito · Crédito (na tela de pagamento) |
| F10 | Finalizar |
| F8 | Cancelar venda |
| Enter · P | Nova venda · imprimir DANFE (após finalizar) |

## Configuração

| Variável | Padrão |
|---|---|
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | `jdbc:postgresql://localhost:5432/balcao` / `balcao` / `balcao` |
| `CORS_ORIGINS` | `http://localhost:5173` |
| `ESTOQUE_POLITICA` | `PERMITIR_E_AVISAR` (ou `BLOQUEAR`) |

## Estrutura

```
backend/src/main/java/br/com/balcao/pdv/
  produto/   cadastro, busca, GTIN
  estoque/   movimentações (única forma de alterar o saldo)
  caixa/     abertura, sangria, suprimento, fechamento e extrato
  venda/     agregado Venda: itens, pagamentos, troco, status
  fiscal/    NFC-e: configuração, chave, XML, QR Code, DANFE, emissores
  comum/     erros padronizados, dinheiro, CPF/CNPJ
frontend/src/
  pages/     Pdv, Caixa, Produtos, Vendas, Fiscal
  components/ Danfe, ExtratoCaixa, Painel
```
