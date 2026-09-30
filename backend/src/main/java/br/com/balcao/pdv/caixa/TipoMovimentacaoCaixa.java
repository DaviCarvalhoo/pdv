package br.com.balcao.pdv.caixa;

public enum TipoMovimentacaoCaixa {
    SUPRIMENTO(1),
    SANGRIA(-1),
    VENDA_DINHEIRO(1),
    ESTORNO_VENDA(-1);

    /** Efeito no saldo físico do caixa. */
    private final int sinal;

    TipoMovimentacaoCaixa(int sinal) {
        this.sinal = sinal;
    }

    public int sinal() {
        return sinal;
    }
}
