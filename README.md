# Balcão PDV

Ponto de venda completo para loja de balcão (mercadinho, padaria, conveniência, loja de bairro).

- **Backend:** Java 21 · Spring Boot 3.4 · Spring Data JPA · PostgreSQL 16 · Flyway · API REST (Problem Details)
- **Frontend:** React 18 · TypeScript · Vite
- **Docs:** [Briefing](docs/BRIEFING.md) · [PRD](docs/PRD.md) · [NFC-e](docs/NFCE.md)

## O que ele faz

**Venda (PDV)**
- Leitor de código de barras, busca por nome, `3*código` para quantidade e `0,350*código` para peso
- Etiqueta de balança (EAN-13 com prefixo 2, com preço ou peso embutido)
- Botões de acesso rápido (pão, cafezinho, sacola…) e consulta de preço (F9)
- Preço promocional com vigência, aplicado sozinho
- Desconto em valor ou % (acima do limite do operador, pede o PIN do gerente)
- Cliente na venda (F5), CPF/CNPJ na nota (F3), venda em espera (F7)
- Pagamento dividido em dinheiro, PIX, débito, crédito, vale-alimentação, vale-refeição e **fiado**
- **QR Code PIX** no valor exato da venda (BR Code com a chave da loja)
- Troco, NFC-e automática e cupom (DANFE) com logo e mensagem da loja

**Gestão**
- **Painel** do dia: faturamento, vendas, ticket médio, lucro bruto e margem, comparados com ontem; vendas por hora, últimos 30 dias, formas de pagamento, categorias, operadores, mais vendidos e alertas
- Caixa: abertura, suprimento, sangria (com gerente), alerta de gaveta cheia, fechamento cego com conferência e extrato
- Produtos com categoria, custo e margem, promoção, estoque mínimo e histórico; etiquetas de gôndola
- **Entrada de mercadoria pelo XML da NF-e** do fornecedor (atualiza custo, cadastra o que falta, dá entrada no estoque)
- Clientes e **fiado** com limite de crédito, conta corrente, recebimento e lembrete por WhatsApp
- Histórico de vendas com filtros, estorno e exportação para Excel (CSV)
- **Curva ABC** de produtos com exportação

**Administração**
- **Loja**: nome, logo, cor (aplicada no sistema inteiro), frase, mensagem do cupom, regras da venda, PIX, balança e tributos aproximados (Lei 12.741)
- **Operadores** com login por PIN e perfis: operador, gerente e administrador. Ações sensíveis pedem o PIN do gerente (autorização de uso único)
- **Fiscal**: NFC-e modelo 65 (chave, XML 4.00 com desconto rateado e tributos, QR Code, DANFE, cancelamento). Vem com emissor **simulado**; ver [docs/NFCE.md](docs/NFCE.md) para emitir de verdade

## Rodando

Pré-requisitos: Java 21, Maven, Node 20+ e Docker.

```bash
# 1. Banco
docker compose up -d

# 2. Backend (perfil demo = loja de exemplo com 30 dias de vendas)
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=demo
# API em http://localhost:8080 · Swagger em http://localhost:8080/swagger-ui.html

# 3. Frontend
cd frontend
npm install
npm run dev
# PDV em http://localhost:5173
```

Sem o perfil `demo`, o sistema sobe vazio e a primeira tela pede a criação do administrador.

### Acessos da demonstração

Só existem com o perfil `demo` (ver `backend/src/main/java/br/com/balcao/pdv/config/DadosDemonstracao.java`):

| Operador | Perfil | PIN |
|---|---|---|
| Dono | Administrador | 1234 |
| Ana | Gerente | 2222 |
| Bruno | Operador | 1111 |
| Carla | Operador | 3333 |

Para recomeçar a demonstração do zero: `docker compose down -v && docker compose up -d` e suba o backend de novo.

## Testes

```bash
cd backend
mvn test   # unitários + integração com PostgreSQL real via Testcontainers (precisa do Docker)
```

## Atalhos

| Tecla | Ação |
|---|---|
| Alt 1…9 | Navega pelas telas do menu |
| F2 | Focar o leitor |
| F3 · F5 · F6 · F7 · F9 | CPF na nota · cliente · desconto · espera · consulta de preço |
| F4 · F10 | Receber · finalizar |
| F8 | Cancelar venda |
| ↑ ↓ · + − · Del | Escolher linha · mudar quantidade · remover |
| D P B C A R F | Forma de pagamento (dinheiro, PIX, débito, crédito, VA, VR, fiado) |
| Enter · P | Nova venda · imprimir cupom (depois de finalizar) |

## Configuração

| Variável | Padrão |
|---|---|
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | `jdbc:postgresql://localhost:5432/balcao` / `balcao` / `balcao` |
| `CORS_ORIGINS` | `http://localhost:5173` |

As regras da venda (estoque, desconto máximo, limite da gaveta, balança, PIX, tributos) ficam na tela **Loja**.

## Estrutura

```
backend/src/main/java/br/com/balcao/pdv/
  produto/    cadastro, categorias, GTIN, promoção
  estoque/    movimentações e entrada por XML de NF-e
  caixa/      abertura, sangria, suprimento, fechamento e extrato
  venda/      agregado Venda: itens, desconto, pagamentos, troco, espera
  cliente/    clientes e conta do fiado
  loja/       identidade, regras, PIX (BR Code) e etiqueta de balança
  usuario/    operadores, login por PIN, perfis e autorização do gerente
  relatorio/  painel e curva ABC
  fiscal/     NFC-e: configuração, chave, XML, QR Code, DANFE, emissores
frontend/src/
  pages/      Login, Pdv, Painel, Caixa, Clientes, Vendas, Produtos, EntradaNfe, Relatorios, Loja, Usuarios, Fiscal
  components/ Danfe, Graficos, PdvPaineis, PdvPagamento, AutorizacaoGerente, ExtratoCaixa, Painel, Marca
```
