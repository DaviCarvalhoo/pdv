package br.com.balcao.pdv.fiscal;

import br.com.balcao.pdv.venda.Venda;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "nota_fiscal")
@Getter
@Setter
@NoArgsConstructor
public class NotaFiscal {

    public static final String MODELO_NFCE = "65";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Venda venda;

    private String modelo = MODELO_NFCE;
    private Integer serie;
    private Integer numero;
    private String chaveAcesso;

    @Enumerated(EnumType.STRING)
    private Ambiente ambiente;

    @Enumerated(EnumType.STRING)
    private StatusNota status;

    private String protocolo;
    private String motivo;
    private OffsetDateTime dataEmissao;
    private OffsetDateTime dataAutorizacao;
    private OffsetDateTime dataCancelamento;
    private String justificativaCancelamento;
    private String protocoloCancelamento;
    private String urlQrCode;
    private String xml;

    public NotaFiscal(Venda venda, int serie, int numero, Ambiente ambiente) {
        this.venda = venda;
        this.serie = serie;
        this.numero = numero;
        this.ambiente = ambiente;
        this.status = StatusNota.PENDENTE;
    }
}
