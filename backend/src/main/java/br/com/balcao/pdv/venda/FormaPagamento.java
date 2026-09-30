package br.com.balcao.pdv.venda;

public enum FormaPagamento {
    DINHEIRO("01"),
    PIX("17"),
    CARTAO_DEBITO("04"),
    CARTAO_CREDITO("03");

    /** Código {@code tPag} do layout da NFC-e. */
    private final String codigoNfce;

    FormaPagamento(String codigoNfce) {
        this.codigoNfce = codigoNfce;
    }

    public String codigoNfce() {
        return codigoNfce;
    }

    public boolean isCartao() {
        return this == CARTAO_DEBITO || this == CARTAO_CREDITO;
    }
}
