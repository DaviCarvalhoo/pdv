/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv.fiscal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Registro único (id = 1) com o emitente e os parâmetros da NFC-e. */
@Entity
@Table(name = "configuracao_fiscal")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracaoFiscal {

    public static final long ID = 1L;

    @Id
    private Long id;

    // Emitente
    private String cnpj;
    private String inscricaoEstadual;
    private String razaoSocial;
    private String nomeFantasia;
    /** 1 = Simples Nacional, 2 = Simples (excesso de sublimite), 3 = Regime Normal. */
    private Integer crt;
    private String logradouro;
    private String numero;
    private String bairro;
    private String codigoMunicipio;
    private String municipio;
    private String uf;
    private String cep;
    private String telefone;

    // Emissão
    @Enumerated(EnumType.STRING)
    private Ambiente ambiente;
    private Integer serie;
    private Integer proximoNumero;
    private String cscId;
    private String csc;
    private String urlQrCode;
    private String urlConsulta;
    private boolean emissaoAutomatica;
    private Integer prazoCancelamentoMin;

    @Enumerated(EnumType.STRING)
    private TipoEmissor emissor;

    /** Consome o próximo número da série. Chamar só com a configuração travada. */
    public int reservarNumero() {
        int numero = proximoNumero;
        proximoNumero = numero + 1;
        return numero;
    }
}
