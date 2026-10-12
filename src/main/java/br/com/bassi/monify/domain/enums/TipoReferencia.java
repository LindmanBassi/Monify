package br.com.bassi.monify.domain.enums;

import java.time.YearMonth;

public enum TipoReferencia {

    /** valor_limite do orçamento da categoria no mês da meta. */
    ORCAMENTO(0),

    /** Gasto total da categoria no mês anterior ao da meta. */
    MES_ANTERIOR(1),

    /** Média do gasto da categoria nos 3 meses anteriores ao da meta. */
    MEDIA_TRES_MESES(3);

    private final int mesesDeHistoricoExigidos;

    TipoReferencia(int mesesDeHistoricoExigidos) {
        this.mesesDeHistoricoExigidos = mesesDeHistoricoExigidos;
    }

    /**
     * Indica se o histórico do usuário cobre os meses anteriores exigidos pela referência.
     * A disponibilidade de {@link #ORCAMENTO} depende da existência do orçamento, não do histórico.
     *
     * @param inicioHistorico mês da primeira transação do usuário; nulo se não houver transações
     */
    public boolean historicoSuficiente(YearMonth mesMeta, YearMonth inicioHistorico) {
        if (mesesDeHistoricoExigidos == 0) {
            return true;
        }
        return inicioHistorico != null
                && !mesMeta.minusMonths(mesesDeHistoricoExigidos).isBefore(inicioHistorico);
    }
}
