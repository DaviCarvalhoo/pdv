package br.com.balcao.pdv.fiscal;

import br.com.balcao.pdv.comum.Documento;
import br.com.balcao.pdv.comum.RegraNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class ConfiguracaoFiscalService {

    private final ConfiguracaoFiscalRepository repository;

    @Transactional(readOnly = true)
    public ConfiguracaoFiscal obter() {
        return repository.findById(ConfiguracaoFiscal.ID)
                .orElseThrow(() -> new IllegalStateException("Configuração fiscal ausente (migração V1)."));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ConfiguracaoFiscal travar() {
        return repository.travar(ConfiguracaoFiscal.ID)
                .orElseThrow(() -> new IllegalStateException("Configuração fiscal ausente (migração V1)."));
    }

    @Transactional
    public ConfiguracaoFiscal atualizar(ConfiguracaoFiscalDto.Request r) {
        ConfiguracaoFiscal c = travar();
        if (StringUtils.hasText(r.cnpj()) && !Documento.cnpjValido(r.cnpj())) {
            throw new RegraNegocioException("CNPJ_INVALIDO", "O CNPJ do emitente é inválido.");
        }
        if (StringUtils.hasText(r.uf()) && !CodigoUf.existe(r.uf())) {
            throw new RegraNegocioException("UF_INVALIDA", "UF inválida: " + r.uf());
        }
        c.setCnpj(texto(r.cnpj()));
        c.setInscricaoEstadual(texto(r.inscricaoEstadual()));
        c.setRazaoSocial(texto(r.razaoSocial()));
        c.setNomeFantasia(texto(r.nomeFantasia()));
        definir(r.crt(), c::setCrt);
        c.setLogradouro(texto(r.logradouro()));
        c.setNumero(texto(r.numero()));
        c.setBairro(texto(r.bairro()));
        c.setCodigoMunicipio(texto(r.codigoMunicipio()));
        c.setMunicipio(texto(r.municipio()));
        c.setUf(StringUtils.hasText(r.uf()) ? r.uf().toUpperCase() : null);
        c.setCep(texto(r.cep()));
        c.setTelefone(texto(r.telefone()));
        definir(r.ambiente(), c::setAmbiente);
        definir(r.serie(), c::setSerie);
        definir(r.proximoNumero(), c::setProximoNumero);
        c.setCscId(texto(r.cscId()));
        if (StringUtils.hasText(r.csc())) {
            c.setCsc(r.csc().trim());
        }
        c.setUrlQrCode(texto(r.urlQrCode()));
        c.setUrlConsulta(texto(r.urlConsulta()));
        definir(r.emissaoAutomatica(), c::setEmissaoAutomatica);
        definir(r.prazoCancelamentoMin(), c::setPrazoCancelamentoMin);
        definir(r.emissor(), c::setEmissor);
        return c;
    }

    /** O que falta para emitir (RN-NFC-03). Lista vazia = pronto. */
    public List<String> pendencias(ConfiguracaoFiscal c) {
        List<String> p = new ArrayList<>();
        exigir(p, c.getCnpj() != null && Documento.cnpjValido(c.getCnpj()), "CNPJ válido");
        exigir(p, StringUtils.hasText(c.getInscricaoEstadual()), "Inscrição estadual");
        exigir(p, StringUtils.hasText(c.getRazaoSocial()), "Razão social");
        exigir(p, StringUtils.hasText(c.getLogradouro()), "Logradouro");
        exigir(p, StringUtils.hasText(c.getNumero()), "Número do endereço");
        exigir(p, StringUtils.hasText(c.getBairro()), "Bairro");
        exigir(p, c.getCodigoMunicipio() != null && c.getCodigoMunicipio().matches("\\d{7}"),
                "Código IBGE do município");
        exigir(p, StringUtils.hasText(c.getMunicipio()), "Município");
        exigir(p, CodigoUf.existe(c.getUf()), "UF");
        exigir(p, c.getCep() != null && c.getCep().matches("\\d{8}"), "CEP");
        exigir(p, StringUtils.hasText(c.getCscId()) && StringUtils.hasText(c.getCsc()), "CSC e ID do CSC");
        exigir(p, StringUtils.hasText(c.getUrlQrCode()), "URL do QR Code da UF");
        exigir(p, StringUtils.hasText(c.getUrlConsulta()), "URL de consulta da UF");
        exigir(p, c.getCrt() != null && c.getCrt() == 1, "Regime Simples Nacional (CRT 1) — outros regimes ainda não são suportados");
        return p;
    }

    void validarCompleta(ConfiguracaoFiscal c) {
        List<String> pendencias = pendencias(c);
        if (!pendencias.isEmpty()) {
            throw new RegraNegocioException("CONFIGURACAO_FISCAL_INCOMPLETA",
                    "Configuração fiscal incompleta: " + String.join(", ", pendencias) + ".",
                    Map.of("pendencias", pendencias));
        }
    }

    private static void exigir(List<String> pendencias, boolean ok, String item) {
        if (!ok) {
            pendencias.add(item);
        }
    }

    private static <T> void definir(T valor, Consumer<T> setter) {
        if (valor != null) {
            setter.accept(valor);
        }
    }

    private static String texto(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
