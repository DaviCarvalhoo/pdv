package br.com.balcao.pdv.estoque;

/** O que fazer quando a venda leva o estoque abaixo de zero (PRD, Q3). */
public enum PoliticaSaldoInsuficiente {
    PERMITIR_E_AVISAR,
    BLOQUEAR
}
