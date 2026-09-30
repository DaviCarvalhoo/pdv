package br.com.balcao.pdv.usuario;

public enum Papel {
    OPERADOR,
    GERENTE,
    ADMIN;

    /** Hierarquia simples: ADMIN pode tudo que GERENTE pode, que pode tudo que OPERADOR pode. */
    public boolean atende(Papel minimo) {
        return ordinal() >= minimo.ordinal();
    }
}
