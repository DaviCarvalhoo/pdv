package br.com.balcao.pdv.venda;

public enum FormaPagamento {
    DINHEIRO("01", "dinheiro"),
    PIX("17", "PIX"),
    CARTAO_DEBITO("04", "débito"),
    CARTAO_CREDITO("03", "crédito"),
    VALE_ALIMENTACAO("10", "vale-alimentação"),
    VALE_REFEICAO("11", "vale-refeição"),
    /** Fiado: vira dívida na conta do cliente. */
    CREDIARIO("05", "fiado"),
    /** Crédito de uma troca anterior; o identificador da transação é o código do vale. */
    VALE_TROCA("99", "vale-troca");

    /** Código {@code tPag} do layout da NFC-e. */
    private final String codigoNfce;
    private final String rotulo;

    FormaPagamento(String codigoNfce, String rotulo) {
        this.codigoNfce = codigoNfce;
        this.rotulo = rotulo;
    }

    public String codigoNfce() {
        return codigoNfce;
    }

    public String rotulo() {
        return rotulo;
    }

    public boolean isCartao() {
        return this == CARTAO_DEBITO || this == CARTAO_CREDITO;
    }
}
