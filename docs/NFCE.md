<!--
  Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
  Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
  Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
  a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
  Autoria: BPDV-7F3A-DC26
-->

# NFC-e no Balcão PDV

## O que já funciona

| Parte | Status |
|---|---|
| Configuração do emitente e da emissão (tela **Fiscal**) | ✅ |
| Dados fiscais por produto (NCM, CFOP, CSOSN, origem) | ✅ |
| Numeração sequencial por série, sem buracos (lock no banco) | ✅ |
| Chave de acesso de 44 dígitos com DV módulo 11 | ✅ |
| XML no layout 4.00 (ide, emit, dest, det, total, transp, pag, infAdic, infNFeSupl) | ✅ |
| QR Code versão 2 com hash SHA-1 do CSC | ✅ |
| DANFE NFC-e 80 mm com QR Code, pronto para imprimir | ✅ |
| CPF/CNPJ do consumidor na nota | ✅ |
| Cancelamento com justificativa e prazo configurável | ✅ |
| Reemissão de nota rejeitada ou pendente, com o mesmo número | ✅ |
| Estorno da venda bloqueado enquanto houver nota autorizada | ✅ |
| Falha na nota **não** desfaz a venda | ✅ |
| **Emissor simulado** (autoriza localmente, só em homologação) | ✅ |
| Assinatura XMLDSig com certificado A1 + transmissão SOAP à SEFAZ | ❌ ver abaixo |
| Contingência offline (tpEmis 9) e inutilização de numeração | ❌ P1 no PRD |

## Por que existe um emissor simulado

Emitir NFC-e de verdade exige coisas que só o dono do CNPJ tem:

1. **Credenciamento** do CNPJ para NFC-e na SEFAZ do estado.
2. **CSC** (Código de Segurança do Contribuinte) e seu ID, gerados no portal da SEFAZ.
3. **Certificado digital A1** (.pfx) da empresa, com a senha.
4. As **URLs de QR Code e de consulta** da UF (cada estado tem as suas, uma para homologação e outra para produção).
5. A orientação do **contador** sobre o regime tributário e o NCM, o CFOP e o CSOSN de cada produto.

Sem esses itens, o sistema monta a nota completa (chave, XML, QR Code, DANFE) e o `EmissorSimulado` responde como a SEFAZ responderia (`cStat 100`, protocolo de 15 dígitos). Isso permite desenvolver, demonstrar e treinar sem risco.

**Proteção:** o emissor simulado **recusa qualquer nota em ambiente de produção** (`cStat 999`). Não há como emitir uma nota "de mentira" em produção por engano.

## Como plugar o emissor real

A transmissão fica atrás da interface `EmissorNfce` (`backend/src/main/java/br/com/balcao/pdv/fiscal/EmissorNfce.java`):

```java
public interface EmissorNfce {
    TipoEmissor tipo();
    Retorno autorizar(NotaFiscal nota, String xml, ConfiguracaoFiscal configuracao);
    Retorno cancelar(NotaFiscal nota, String justificativa, ConfiguracaoFiscal configuracao);
}
```

Há dois caminhos:

**A) Provedor de NFC-e (mais simples):** Focus NFe, Nuvem Fiscal, PlugNotas, eNotas e outros cuidam da assinatura, da comunicação com a SEFAZ, da contingência e das mudanças de layout. O `EmissorProvedor` só chama a API REST deles e devolve o `Retorno`. O certificado fica cadastrado no provedor. Há custo por nota ou mensalidade.

**B) SEFAZ direto:**
1. Assinar o `infNFe` com XMLDSig (RSA-SHA1, C14N) usando o A1. O JDK já traz `javax.xml.crypto.dsig`. A biblioteca open source **Java_NFe** (Samuel Oliveira) resolve assinatura e webservices.
2. Enviar para o webservice `NFeAutorizacao4` da UF por SOAP 1.2 com TLS mútuo (o próprio A1).
3. Tratar `cStat` (100 = autorizada; 1xx/2xx/3xx/7xx = rejeições) e guardar o `protNFe`.
4. Implementar o evento de cancelamento (`110111`) em `RecepcaoEvento4`.

Nos dois casos:
1. Crie a classe anotada com `@Component` implementando `EmissorNfce`.
2. Acrescente o valor em `TipoEmissor` (ex.: `PROVEDOR` ou `SEFAZ`).
3. Selecione o emissor na configuração fiscal.
4. Guarde senhas e tokens em variáveis de ambiente, nunca no repositório (`*.pfx` já está no `.gitignore`).

## Limitações atuais do gerador de XML

- Regime **Simples Nacional (CRT 1)** apenas. CSOSN suportados: 102, 103, 300, 400 e 500.
- PIS/COFINS saem com CST 49 e valores zerados, o usual no Simples. **Confirme com o contador.**
- Sem desconto, frete ou outras despesas no XML (o PDV ainda não tem desconto).
- Sem cálculo de "tributos aproximados" (Lei 12.741/2012, IBPT).

## Checklist para ir para produção

- [ ] CNPJ credenciado para NFC-e na SEFAZ da UF
- [ ] CSC de **produção** gerado e cadastrado (o de homologação é outro)
- [ ] URLs de QR Code e consulta de **produção** da UF
- [ ] Certificado A1 válido instalado no emissor real ou no provedor
- [ ] Emissor real implementado e testado em **homologação**
- [ ] NCM/CFOP/CSOSN de todos os produtos revisados pelo contador
- [ ] Ambiente trocado para `PRODUCAO` e série/numeração conferidas
