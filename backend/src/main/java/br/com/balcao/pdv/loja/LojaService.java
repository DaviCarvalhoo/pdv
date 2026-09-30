package br.com.balcao.pdv.loja;

import br.com.balcao.pdv.comum.RegraNegocioException;
import br.com.balcao.pdv.estoque.PoliticaSaldoInsuficiente;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class LojaService {

    /** ~300 KB de imagem viram ~400 KB em base64. */
    private static final int TAMANHO_MAX_LOGO = 420_000;

    private final LojaRepository repository;

    public record LojaRequest(String nomeFantasia, String slogan, String logo, String corDestaque,
                              String mensagemCupom, PoliticaSaldoInsuficiente politicaEstoque,
                              BigDecimal limiteGaveta, BigDecimal descontoMaxOperador, String balancaPrefixo,
                              Integer balancaDigitosCodigo, TipoValorBalanca balancaTipoValor, String chavePix,
                              String pixRecebedor, String pixCidade, BigDecimal aliquotaTributos) {
    }

    @Transactional(readOnly = true)
    public Loja obter() {
        return repository.findById(Loja.ID).orElseThrow(() -> new IllegalStateException("Loja ausente (migração V2)."));
    }

    @Transactional
    public Loja atualizar(LojaRequest r) {
        Loja l = obter();
        if (!StringUtils.hasText(r.nomeFantasia())) {
            throw new RegraNegocioException("NOME_OBRIGATORIO", "Informe o nome da loja.");
        }
        if (r.corDestaque() != null && !r.corDestaque().matches("#[0-9A-Fa-f]{6}")) {
            throw new RegraNegocioException("COR_INVALIDA", "A cor deve estar no formato #RRGGBB.");
        }
        if (r.logo() != null && !r.logo().isEmpty()) {
            if (!r.logo().matches("^data:image/(png|jpeg|webp|svg\\+xml);base64,[A-Za-z0-9+/=]+$")) {
                throw new RegraNegocioException("LOGO_INVALIDO", "Envie o logo em PNG, JPG, WEBP ou SVG.");
            }
            if (r.logo().length() > TAMANHO_MAX_LOGO) {
                throw new RegraNegocioException("LOGO_GRANDE", "O logo deve ter no máximo 300 KB.");
            }
        }
        exigirFaixa(r.descontoMaxOperador(), "DESCONTO_INVALIDO", "O desconto máximo deve ficar entre 0 e 100%.");
        exigirFaixa(r.aliquotaTributos(), "ALIQUOTA_INVALIDA", "A alíquota deve ficar entre 0 e 100%.");
        if (r.balancaDigitosCodigo() != null && (r.balancaDigitosCodigo() < 4 || r.balancaDigitosCodigo() > 6)) {
            throw new RegraNegocioException("BALANCA_INVALIDA", "O código na etiqueta da balança tem de 4 a 6 dígitos.");
        }
        if (r.balancaPrefixo() != null && !r.balancaPrefixo().matches("\\d")) {
            throw new RegraNegocioException("BALANCA_INVALIDA", "O prefixo da balança é um dígito (normalmente 2).");
        }
        if (StringUtils.hasText(r.chavePix()) && (!StringUtils.hasText(r.pixRecebedor()) || !StringUtils.hasText(r.pixCidade()))) {
            throw new RegraNegocioException("PIX_INCOMPLETO", "Para gerar o QR Code PIX informe também o nome do recebedor e a cidade.");
        }

        l.setNomeFantasia(r.nomeFantasia().trim());
        l.setSlogan(texto(r.slogan()));
        l.setLogo(r.logo() == null || r.logo().isEmpty() ? null : r.logo());
        if (r.corDestaque() != null) {
            l.setCorDestaque(r.corDestaque().toUpperCase());
        }
        l.setMensagemCupom(texto(r.mensagemCupom()));
        if (r.politicaEstoque() != null) {
            l.setPoliticaEstoque(r.politicaEstoque());
        }
        l.setLimiteGaveta(r.limiteGaveta() != null && r.limiteGaveta().signum() > 0 ? r.limiteGaveta() : null);
        if (r.descontoMaxOperador() != null) {
            l.setDescontoMaxOperador(r.descontoMaxOperador());
        }
        if (r.balancaPrefixo() != null) {
            l.setBalancaPrefixo(r.balancaPrefixo());
        }
        if (r.balancaDigitosCodigo() != null) {
            l.setBalancaDigitosCodigo(r.balancaDigitosCodigo());
        }
        if (r.balancaTipoValor() != null) {
            l.setBalancaTipoValor(r.balancaTipoValor());
        }
        l.setChavePix(texto(r.chavePix()));
        l.setPixRecebedor(texto(r.pixRecebedor()));
        l.setPixCidade(texto(r.pixCidade()));
        l.setAliquotaTributos(r.aliquotaTributos());
        return l;
    }

    /** BR Code do PIX no valor informado, ou vazio se a loja não configurou a chave. */
    @Transactional(readOnly = true)
    public String pix(BigDecimal valor, String txid) {
        Loja l = obter();
        if (!StringUtils.hasText(l.getChavePix())) {
            throw new RegraNegocioException("PIX_NAO_CONFIGURADO", "Cadastre a chave PIX da loja em Loja > Pagamentos.");
        }
        return PixBrCode.gerar(l.getChavePix(), l.getPixRecebedor(), l.getPixCidade(), valor, txid);
    }

    private static void exigirFaixa(BigDecimal v, String codigo, String msg) {
        if (v != null && (v.signum() < 0 || v.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new RegraNegocioException(codigo, msg);
        }
    }

    private static String texto(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
