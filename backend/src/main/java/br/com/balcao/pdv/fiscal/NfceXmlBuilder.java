package br.com.balcao.pdv.fiscal;

import br.com.balcao.pdv.comum.RegraNegocioException;
import br.com.balcao.pdv.produto.Produto;
import br.com.balcao.pdv.venda.FormaPagamento;
import br.com.balcao.pdv.venda.ItemVenda;
import br.com.balcao.pdv.venda.Pagamento;
import br.com.balcao.pdv.venda.Rateio;
import br.com.balcao.pdv.venda.Venda;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/**
 * Monta o XML da NFC-e no layout 4.00 (sem assinatura). A assinatura XMLDSig com o certificado A1 é
 * responsabilidade do emissor real, entre {@code infNFe} e {@code infNFeSupl}.
 *
 * <p>Tributação suportada: Simples Nacional (CRT 1) com CSOSN 102, 103, 300, 400 e 500. PIS/COFINS saem
 * com CST 49 e valores zerados, o usual no Simples — confirme com o contador.
 */
public final class NfceXmlBuilder {

    public static final String XMLNS = "http://www.portalfiscal.inf.br/nfe";
    public static final String TEXTO_HOMOLOGACAO =
            "NOTA FISCAL EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL";
    static final Set<String> CSOSN_SUPORTADOS = Set.of("102", "103", "300", "400", "500");

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final StringBuilder xml = new StringBuilder(4096);

    private NfceXmlBuilder() {
    }

    public record Dados(ConfiguracaoFiscal emitente, Venda venda, NotaFiscal nota, String codigoNumerico,
                        OffsetDateTime emissao, String urlQrCode, BigDecimal aliquotaTributosPadrao) {
    }

    public static String montar(Dados d) {
        return new NfceXmlBuilder().gerar(d);
    }

    private String gerar(Dados d) {
        ConfiguracaoFiscal e = d.emitente();
        Venda v = d.venda();
        NotaFiscal n = d.nota();
        String chave = n.getChaveAcesso();
        boolean homologacao = n.getAmbiente() == Ambiente.HOMOLOGACAO;

        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        abrir("NFe xmlns=\"" + XMLNS + "\"");
        abrir("infNFe versao=\"4.00\" Id=\"NFe" + chave + "\"");

        abrir("ide");
        tag("cUF", CodigoUf.de(e.getUf()));
        tag("cNF", d.codigoNumerico());
        tag("natOp", "VENDA");
        tag("mod", NotaFiscal.MODELO_NFCE);
        tag("serie", String.valueOf(n.getSerie()));
        tag("nNF", String.valueOf(n.getNumero()));
        tag("dhEmi", d.emissao().format(DATA_HORA));
        tag("tpNF", "1");
        tag("idDest", "1");
        tag("cMunFG", e.getCodigoMunicipio());
        tag("tpImp", "4");
        tag("tpEmis", "1");
        tag("cDV", chave.substring(43));
        tag("tpAmb", n.getAmbiente().codigo());
        tag("finNFe", "1");
        tag("indFinal", "1");
        tag("indPres", "1");
        tag("procEmi", "0");
        tag("verProc", "BalcaoPDV 1.0");
        fechar("ide");

        abrir("emit");
        tag("CNPJ", e.getCnpj());
        tag("xNome", homologacao ? TEXTO_HOMOLOGACAO : e.getRazaoSocial());
        tagOpcional("xFant", e.getNomeFantasia());
        abrir("enderEmit");
        tag("xLgr", e.getLogradouro());
        tag("nro", e.getNumero());
        tag("xBairro", e.getBairro());
        tag("cMun", e.getCodigoMunicipio());
        tag("xMun", e.getMunicipio());
        tag("UF", e.getUf());
        tag("CEP", e.getCep());
        tag("cPais", "1058");
        tag("xPais", "BRASIL");
        tagOpcional("fone", e.getTelefone());
        fechar("enderEmit");
        tag("IE", e.getInscricaoEstadual());
        tag("CRT", String.valueOf(e.getCrt()));
        fechar("emit");

        if (v.getDocumentoConsumidor() != null) {
            abrir("dest");
            tag(v.getDocumentoConsumidor().length() == 11 ? "CPF" : "CNPJ", v.getDocumentoConsumidor());
            if (homologacao) {
                tag("xNome", TEXTO_HOMOLOGACAO);
            }
            tag("indIEDest", "9");
            fechar("dest");
        }

        List<BigDecimal> descontos = Rateio.descontos(v);
        List<BigDecimal> tributos = Rateio.tributos(v, d.aliquotaTributosPadrao());
        BigDecimal totalTributos = Rateio.soma(tributos);
        for (int i = 0; i < v.getItens().size(); i++) {
            detalhe(i + 1, v.getItens().get(i), descontos.get(i), tributos.get(i), homologacao && i == 0);
        }

        abrir("total");
        abrir("ICMSTot");
        for (String campo : new String[]{"vBC", "vICMS", "vICMSDeson", "vFCP", "vBCST", "vST", "vFCPST",
                "vFCPSTRet"}) {
            tag(campo, "0.00");
        }
        tag("vProd", valor(v.getSubtotal()));
        tag("vFrete", "0.00");
        tag("vSeg", "0.00");
        tag("vDesc", valor(v.getDesconto()));
        for (String campo : new String[]{"vII", "vIPI", "vIPIDevol", "vPIS", "vCOFINS", "vOutro"}) {
            tag(campo, "0.00");
        }
        tag("vNF", valor(v.getTotal()));
        if (totalTributos.signum() > 0) {
            tag("vTotTrib", valor(totalTributos));
        }
        fechar("ICMSTot");
        fechar("total");

        abrir("transp");
        tag("modFrete", "9");
        fechar("transp");

        abrir("pag");
        for (Pagamento p : v.getPagamentos()) {
            abrir("detPag");
            tag("tPag", p.getForma().codigoNfce());
            if ("99".equals(p.getForma().codigoNfce())) {
                tag("xPag", "Vale-troca");
            }
            tag("vPag", valor(p.getValor()));
            if (p.getForma().isCartao() || p.getForma() == FormaPagamento.PIX) {
                // 2 = pagamento não integrado ao sistema de automação (maquininha/PIX lançados à mão).
                abrir("card");
                tag("tpIntegra", "2");
                fechar("card");
            }
            fechar("detPag");
        }
        if (v.getTroco().signum() > 0) {
            tag("vTroco", valor(v.getTroco()));
        }
        fechar("pag");

        abrir("infAdic");
        String complemento = "Venda #" + v.getId() + " - Caixa #" + v.getCaixa().getId();
        if (totalTributos.signum() > 0) {
            complemento += " - Tributos totais aproximados R$ " + valor(totalTributos) + " (Lei 12.741/2012)";
        }
        tag("infCpl", complemento);
        fechar("infAdic");

        fechar("infNFe");

        abrir("infNFeSupl");
        xml.append("<qrCode><![CDATA[").append(d.urlQrCode()).append("]]></qrCode>");
        tag("urlChave", e.getUrlConsulta());
        fechar("infNFeSupl");

        fechar("NFe");
        return xml.toString();
    }

    private void detalhe(int numero, ItemVenda item, BigDecimal desconto, BigDecimal tributos,
                         boolean textoHomologacao) {
        Produto p = item.getProduto();
        String codigo = p.getCodigoInterno() != null ? p.getCodigoInterno() : String.valueOf(p.getId());
        String gtin = p.getGtin() != null ? p.getGtin() : "SEM GTIN";

        abrir("det nItem=\"" + numero + "\"");
        abrir("prod");
        tag("cProd", codigo);
        tag("cEAN", gtin);
        tag("xProd", textoHomologacao ? TEXTO_HOMOLOGACAO : item.getDescricao());
        tag("NCM", p.getNcm());
        tag("CFOP", p.getCfop());
        tag("uCom", p.getUnidade());
        tag("qCom", quantidade(item.getQuantidade()));
        tag("vUnCom", unitario(item.getPrecoUnitario()));
        tag("vProd", valor(item.getSubtotal()));
        tag("cEANTrib", gtin);
        tag("uTrib", p.getUnidade());
        tag("qTrib", quantidade(item.getQuantidade()));
        tag("vUnTrib", unitario(item.getPrecoUnitario()));
        if (desconto.signum() > 0) {
            tag("vDesc", valor(desconto));
        }
        tag("indTot", "1");
        fechar("prod");

        abrir("imposto");
        if (tributos.signum() > 0) {
            tag("vTotTrib", valor(tributos));
        }
        abrir("ICMS");
        String grupo = "500".equals(p.getCsosn()) ? "ICMSSN500" : "ICMSSN102";
        abrir(grupo);
        tag("orig", String.valueOf(p.getOrigem()));
        tag("CSOSN", p.getCsosn());
        fechar(grupo);
        fechar("ICMS");
        abrir("PIS");
        abrir("PISOutr");
        tag("CST", "49");
        tag("vBC", "0.00");
        tag("pPIS", "0.0000");
        tag("vPIS", "0.00");
        fechar("PISOutr");
        fechar("PIS");
        abrir("COFINS");
        abrir("COFINSOutr");
        tag("CST", "49");
        tag("vBC", "0.00");
        tag("pCOFINS", "0.0000");
        tag("vCOFINS", "0.00");
        fechar("COFINSOutr");
        fechar("COFINS");
        fechar("imposto");
        fechar("det");
    }

    /** Recusa produtos que o gerador não sabe tributar, antes de consumir número. */
    static void validarProduto(Produto p) {
        if (p.getNcm() == null || !p.getNcm().matches("\\d{8}")) {
            throw new RegraNegocioException("PRODUTO_SEM_DADOS_FISCAIS",
                    "O produto " + p.getNome() + " não tem NCM válido (8 dígitos).");
        }
        if (!CSOSN_SUPORTADOS.contains(p.getCsosn())) {
            throw new RegraNegocioException("PRODUTO_SEM_DADOS_FISCAIS",
                    "CSOSN " + p.getCsosn() + " do produto " + p.getNome() + " ainda não é suportado.");
        }
    }

    private void abrir(String tagComAtributos) {
        xml.append('<').append(tagComAtributos).append('>');
    }

    private void fechar(String tag) {
        xml.append("</").append(tag).append('>');
    }

    private void tag(String nome, String valor) {
        xml.append('<').append(nome).append('>').append(escapar(valor)).append("</").append(nome).append('>');
    }

    private void tagOpcional(String nome, String valor) {
        if (valor != null && !valor.isBlank()) {
            tag(nome, valor);
        }
    }

    private static String escapar(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String valor(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN).toPlainString();
    }

    private static String unitario(BigDecimal v) {
        return v.setScale(10, RoundingMode.HALF_EVEN).toPlainString();
    }

    private static String quantidade(BigDecimal q) {
        return q.setScale(4, RoundingMode.HALF_EVEN).toPlainString();
    }
}
