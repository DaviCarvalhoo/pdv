package br.com.balcao.pdv.fiscal;

public enum StatusNota {
    /** Montada, mas a transmissão falhou (ex.: SEFAZ fora do ar). Pode ser reemitida. */
    PENDENTE,
    AUTORIZADA,
    /** A SEFAZ recusou. Corrija os dados e reemita com o mesmo número. */
    REJEITADA,
    CANCELADA
}
