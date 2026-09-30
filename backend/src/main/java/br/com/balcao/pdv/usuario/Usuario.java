package br.com.balcao.pdv.usuario;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Enumerated(EnumType.STRING)
    private Papel papel;

    private String pinHash;
    private boolean ativo = true;
    private OffsetDateTime criadoEm;
    private OffsetDateTime ultimoAcesso;

    public Usuario(String nome, Papel papel, String pinHash) {
        this.nome = nome;
        this.papel = papel;
        this.pinHash = pinHash;
    }

    @PrePersist
    void aoCriar() {
        criadoEm = OffsetDateTime.now();
    }
}
