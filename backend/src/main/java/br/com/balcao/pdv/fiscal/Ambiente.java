package br.com.balcao.pdv.fiscal;

public enum Ambiente {
    PRODUCAO("1"),
    HOMOLOGACAO("2");

    /** Código {@code tpAmb} do layout. */
    private final String codigo;

    Ambiente(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
