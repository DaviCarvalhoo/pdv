<!--
  Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
  Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
  Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
  a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
  Autoria: BPDV-7F3A-DC26
-->

# Instalando o Balcão PDV na loja

## Como os caixas funcionam

```
                 ┌───────────────────────────────┐
                 │  PC do caixa (servidor)       │
                 │  banco de dados + sistema     │
                 │  http://localhost:8080        │  ← Caixa 01 vende aqui mesmo
                 └───────────────┬───────────────┘
                                 │ rede da loja (Wi-Fi ou cabo)
               ┌─────────────────┴─────────────────┐
     ┌─────────┴─────────┐               ┌─────────┴─────────┐
     │ Notebook/tablet   │               │ Celular do dono   │
     │ Caixa 02          │               │ Retaguarda        │
     │ http://IP:8080    │               │ (painel, relatórios)│
     └───────────────────┘               └───────────────────┘
```

- **Um computador é o servidor.** Ele guarda o banco de dados e roda o sistema. Numa loja pequena (como uma loja de churros), é o próprio computador do caixa.
- **Cada aparelho que vende é um caixa** ("Caixa 01", "Caixa 02"…), com **gaveta, abertura e fechamento próprios**. Na primeira vez que o sistema é aberto num aparelho, ele pergunta **"Qual caixa é este computador?"**, e a escolha fica gravada naquele aparelho.
- **Os outros aparelhos não instalam nada:** abrem o navegador em `http://IP-DO-SERVIDOR:8080`. O endereço aparece em `logs\supervisor.log`.
- **Produtos, clientes, estoque e relatórios são um só** para a loja inteira. As vendas de cada caixa caem na gaveta daquele caixa, e o painel mostra o total e o total **por caixa**.
- Um aparelho só da gerência (sem vender) pode ser marcado como **Retaguarda**.
- Os caixas são cadastrados em **Loja → Caixas**. Numa loja de um caixa só, não é preciso configurar nada: o "Caixa 01" já vem pronto.
- **Quem abre e fecha o caixa:** por padrão, só gerente ou administrador. Se o operador estiver no balcão, o sistema pede o **PIN do gerente** na hora. Dá para liberar para todos em **Loja → Regras da venda**.
- **Trocar de operador não fecha o caixa:** a gaveta é do computador, não da pessoa. Um operador sai (ícone de sair ou "Trocar de operador" na tela bloqueada), o outro entra e continua vendendo na mesma gaveta. Cada venda registra quem vendeu.
- **Trocar qual caixa é este computador:** o gerente usa **Caixa → "trocar"** (ao lado de "Este computador: Caixa 01").

## Instalação (uma vez, no computador servidor)

Pré-requisitos: **Docker Desktop**, **Java 21**, **Maven** e **Node.js 20+**.

```powershell
cd C:\caminho\do\pdv
.\instalacao\compilar.ps1                  # gera a tela e o servidor
.\instalacao\instalar-inicializacao.ps1    # como administrador: liga com o Windows + backup diário
```

Depois disso, basta ligar o computador: o sistema sobe sozinho em `http://localhost:8080`.

Para testar sem instalar: `.\instalacao\iniciar-balcao.ps1 -Demo` (loja de demonstração, todos os PINs `1234`).

## O que acontece quando algo dá errado

| Situação | O que o sistema faz |
|---|---|
| **Operador sai de perto do caixa** | Depois de 10 min sem uso (configurável em Loja), a tela **trava** e pede o PIN. A venda em andamento não se perde. |
| **Sessão expirou / acesso removido** | Volta para o login **explicando o motivo**. A venda em andamento continua salva e reaparece ao entrar de novo. A sessão se renova sozinha enquanto o operador trabalha e só expira depois de 12 h sem nenhum uso. |
| **Wi-Fi caiu / servidor desligou** | Aparece a faixa vermelha **"Sem conexão com o servidor"**, e o sistema tenta reconectar a cada 3 s. Quando o servidor volta, a faixa some sozinha, **o operador continua logado** (a sessão fica no banco, não no servidor) e a tela **recarrega a venda em andamento**. Só a operação que falhou durante a queda precisa ser repetida. Testado: servidor desligado no meio de uma venda e religado; voltou em 13 s, sem perder a venda e sem pedir login. |
| **A conexão caiu bem na hora de finalizar** | Ao repetir, o sistema **confere se a venda já foi gravada** antes de cobrar de novo. A finalização é atômica: ou grava tudo (estoque, caixa, fiado), ou não grava nada. |
| **Clique duplo em Finalizar** | Ignorado: a venda é finalizada uma vez só. |
| **Navegador travou / fechou / F5** | Tudo está no servidor: ao abrir de novo, a venda em andamento continua de onde parou. |
| **Erro inesperado numa tela** | Em vez de tela branca, aparece "Algo deu errado nesta tela · Nada foi perdido" com o botão **Recarregar**. |
| **O servidor caiu** | O `iniciar-balcao.ps1` o **reinicia sozinho em 5 s** e registra o ocorrido em `logs\supervisor.log`. |
| **Queda de energia / reiniciou o PC** | O banco e o sistema **sobem sozinhos** com o Windows. Vendas finalizadas nunca se perdem (o banco grava em disco a cada transação). |
| **Desligar no meio de uma venda** | O servidor termina as requisições em andamento antes de desligar (desligamento suave). |
| **Computador quebrou** | Restaure o **backup diário** (abaixo) em outro computador. |

## Backup e restauração

O backup roda sozinho todo dia às 23:30 e guarda 30 dias em `backups\`. Para fazer um na hora: `.\instalacao\backup.ps1`.

**Guarde uma cópia fora do computador** (pendrive, Google Drive): backup que fica no mesmo disco não protege contra disco queimado.

Para restaurar num computador novo, com o sistema instalado e o banco no ar:

```powershell
docker cp .\backups\balcao-AAAA-MM-DD_HHMM.dump balcao-postgres:/tmp/restaurar.dump
docker exec balcao-postgres pg_restore -U balcao -d balcao --clean --if-exists /tmp/restaurar.dump
```

## Atualizando o sistema

```powershell
git pull
.\instalacao\compilar.ps1
# reinicie o computador, ou feche o processo "java" que o sistema sobe sozinho de novo
```

O banco é atualizado automaticamente (migrações Flyway) sem perder dados.
