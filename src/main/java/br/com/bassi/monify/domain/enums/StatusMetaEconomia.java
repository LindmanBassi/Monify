package br.com.bassi.monify.domain.enums;

/**
 * Calculado; nunca armazenado.
 */
public enum StatusMetaEconomia {
    CANCELADA,
    AGUARDANDO_INICIO,
    NAO_ATINGIDA,
    ATINGIDA,
    EM_ANDAMENTO;

    public boolean isPendente() {
        return this == AGUARDANDO_INICIO || this == EM_ANDAMENTO;
    }
}
