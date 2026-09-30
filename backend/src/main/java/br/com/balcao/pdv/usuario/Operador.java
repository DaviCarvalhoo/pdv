package br.com.balcao.pdv.usuario;

/** Quem está usando o sistema nesta requisição (vem do token da sessão). */
public record Operador(Long id, String nome, Papel papel) {

    public static Operador de(Usuario u) {
        return new Operador(u.getId(), u.getNome(), u.getPapel());
    }

    public boolean pode(Papel minimo) {
        return papel.atende(minimo);
    }
}
