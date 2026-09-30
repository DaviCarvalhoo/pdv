<!--
  Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
  Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
  Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
  a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
  Autoria: BPDV-7F3A-DC26
-->

# Regras de instalação na loja

Checklist para deixar o Balcão PDV **pronto para o dia a dia**: liga sozinho, aguenta queda de energia e não perde venda.
Siga na ordem e marque cada item. O funcionamento do sistema está explicado no [LEIA-ME](LEIA-ME.md).

> **Regra de ouro:** só libere o caixa para vender depois de passar no **teste da tomada** (etapa 8).

---

## 1. O computador

- [ ] **Windows 10 ou 11, 64 bits**, com a virtualização ligada na BIOS (*Intel VT-x* / *AMD-V*). O Docker precisa dela.
- [ ] **8 GB de memória** no mínimo (o Docker usa uma parte) e **SSD** (liga mais rápido e aguenta melhor queda de energia que HD).
- [ ] **Espaço livre:** 20 GB ou mais.
- [ ] **Cabo de rede** no computador servidor, se houver outros caixas. Wi-Fi funciona, mas cai mais.
- [ ] Um **usuário do Windows só para a loja** (ex.: `caixa`), com senha. Não use a conta pessoal de ninguém.

## 2. Energia

- [ ] **Nobreak (UPS)** ligado no computador, no monitor e no roteador. **É o item mais importante:** a loja continua vendendo e ninguém perde a venda que está sendo passada. 600 VA seguram de 10 a 15 minutos.
- [ ] A impressora e a gaveta podem ficar fora do nobreak, se ele for pequeno.
- [ ] **BIOS/UEFI:** em *Power*, ative **"Restore on AC Power Loss" = Power On** (o nome muda por fabricante: *AC Recovery*, *After Power Failure*). Assim o PC **liga sozinho** quando a energia volta.

## 3. Windows

- [ ] **Entrar sem digitar senha**, para o sistema subir sozinho depois de uma queda. Rode `netplwiz`, selecione o usuário da loja, desmarque *"Os usuários devem digitar um nome de usuário e uma senha"* e confirme a senha.
- [ ] **Nunca suspender nem hibernar:** *Configurações → Sistema → Energia*, "Suspender" = **Nunca**. A tela pode desligar, o computador não.
- [ ] **Desligar a inicialização rápida:** *Painel de Controle → Opções de Energia → Escolher a função dos botões* → desmarque **"Ligar inicialização rápida"**. Ela atrapalha o Docker depois de desligar.
- [ ] **Windows Update fora do horário da loja:** *Windows Update → Opções avançadas → Horário ativo*, por exemplo das 7h às 23h. Assim ele não reinicia no meio do expediente.
- [ ] **Data, hora e fuso automáticos** (*Configurações → Hora e idioma*). Vendas e notas usam a hora deste computador.
- [ ] **Antivírus:** adicione exceções para a pasta do sistema (ex.: `C:\balcao`) e para o Docker, para não deixar o banco lento.

## 4. Programas

Instale nesta ordem e **reinicie o computador no fim**:

- [ ] **Docker Desktop.** Em *Settings → General*, marque **"Start Docker Desktop when you sign in"**. Na primeira abertura, aceite a instalação do WSL 2.
- [ ] **Java 21**, por exemplo o Temurin 21 (adoptium.net). No instalador, marque **"Set JAVA_HOME"**.
- [ ] **Maven 3.9+** no `PATH`.
- [ ] **Node.js 20+** (LTS).
- [ ] **Git.**

Confira num PowerShell novo. Todos precisam responder, sem erro:

```powershell
docker info; java -version; mvn -v; node -v; git --version
```

## 5. Sistema

Use uma pasta **curta e sem espaços nem acentos**, como `C:\balcao`:

```powershell
git clone git@github.com:DaviCarvalhoo/pdv.git C:\balcao
cd C:\balcao
.\instalacao\compilar.ps1
```

- [ ] Rode **como administrador** (botão direito no PowerShell → *Executar como administrador*):

```powershell
Set-ExecutionPolicy -Scope LocalMachine RemoteSigned   # uma vez: libera os scripts do sistema
.\instalacao\instalar-inicializacao.ps1
```

Esse comando cria:

- **inicialização com o Windows**, com reinício automático se o servidor cair e a tela do caixa abrindo sozinha;
- **backup todo dia às 23:30** (ou assim que o PC ligar, se estava desligado);
- a **liberação da porta 8080** para os outros caixas da rede.

| Situação | Comando |
|---|---|
| Computador que é **só servidor** (não vende) | `.\instalacao\instalar-inicializacao.ps1 -SemTela` |
| Loja de **demonstração** (todos os PINs `1234`) | `.\instalacao\instalar-inicializacao.ps1 -Demo` |
| **Desfazer** | `Unregister-ScheduledTask -TaskName "Balcao PDV*"` |

- [ ] **Reinicie o computador.** Em até 2 minutos a tela do caixa abre sozinha.

## 6. Primeiro acesso

- [ ] Crie o **administrador** (nome e PIN) na primeira tela. **Não use `1234`** numa loja de verdade.
- [ ] **Loja:** nome, logo, cor, mensagem do cupom, PIX e regras da venda (quem abre o caixa, desconto máximo, limite da gaveta).
- [ ] **Loja → Caixas:** um caixa para cada aparelho que vende.
- [ ] **Usuários:** um por pessoa, cada um com seu PIN. **Nunca compartilhe PIN.**
- [ ] **Produtos:** cadastro com NCM (necessário para a NFC-e).
- [ ] **Fiscal:** dados da empresa. Até ter o certificado A1 e o CSC da SEFAZ, o emissor fica **simulado** (não vale como nota real).

## 7. Outros caixas (se houver)

- [ ] **IP fixo para o computador servidor.** Reserve o IP no roteador (*DHCP reservation*) para o endereço não mudar.
- [ ] Veja o endereço em `logs\supervisor.log` (linha *"outros caixas da rede: http://IP:8080"*).
- [ ] Em cada aparelho, abra `http://IP-DO-SERVIDOR:8080` no navegador, escolha **qual caixa ele é** e crie um atalho na área de trabalho.
- [ ] **Não libere a porta 5432** (banco de dados) no firewall. Só a 8080 precisa ficar aberta.

## 8. Teste da tomada (obrigatório)

Faça antes de liberar o caixa:

1. [ ] Abra o caixa e comece uma venda com 2 produtos. **Não finalize.**
2. [ ] **Tire o computador da tomada** (ou desligue o nobreak). Não use o botão do Windows.
3. [ ] Espere 10 segundos e ligue de novo.
4. [ ] Confira:
   - o PC ligou sozinho (se a BIOS estiver certa);
   - entrou no Windows sem senha;
   - a tela do caixa abriu sozinha;
   - depois do PIN, **a venda voltou com os 2 produtos**.
5. [ ] Finalize a venda e confira o troco, o estoque e a nota em **Fiscal**.
6. [ ] Veja em `logs\supervisor.log` a sequência *Banco de dados no ar → Iniciando o servidor*.

Se algum item falhar, volte à etapa correspondente: BIOS na 2, `netplwiz` na 3, Docker na 4, tarefa agendada na 5.

## 9. Backup fora do computador

- [ ] Rode `.\instalacao\backup.ps1` e confira o arquivo em `backups\`.
- [ ] **Toda semana**, copie a pasta `backups\` para **fora do computador** (pendrive ou Google Drive/OneDrive). Backup no mesmo disco não salva de disco queimado ou roubo.
- [ ] **Uma vez por mês**, teste a restauração num outro computador (passo a passo no [LEIA-ME](LEIA-ME.md#backup-e-restauração)).

## 10. Regras do dia a dia (para a equipe)

- **Abrir o caixa** ao chegar e **fechar** ao sair, contando a gaveta. Caixa aberto de ontem gera aviso no topo da tela.
- **Não desligar o computador no botão** nem tirar da tomada. Se precisar desligar: *Iniciar → Desligar*.
- **Faltou luz:** espere voltar, entre com o PIN e **confira com o cliente** os itens da venda que reapareceu.
- **PIX ou cartão pago na hora da queda:** confira o aplicativo do banco ou a maquininha **antes de cobrar de novo**.
- **Aviso vermelho de relógio atrasado:** chame o responsável e **não venda** até acertar a hora do Windows.
- **Faixa vermelha "Sem conexão":** não feche a tela. Ela volta sozinha; se passar de 1 minuto, veja se o computador servidor está ligado.
- **Atualizar o sistema** só com a loja fechada: `git pull`, `.\instalacao\compilar.ps1` e reiniciar o computador.
