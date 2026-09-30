package br.com.balcao.pdv.usuario;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "sessao")
@Getter
@NoArgsConstructor
public class Sessao {

    @Id
    private String token;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Usuario usuario;

    private OffsetDateTime criadaEm;
    private OffsetDateTime expiraEm;

    public Sessao(String token, Usuario usuario, OffsetDateTime agora, OffsetDateTime expiraEm) {
        this.token = token;
        this.usuario = usuario;
        this.criadaEm = agora;
        this.expiraEm = expiraEm;
    }
}
