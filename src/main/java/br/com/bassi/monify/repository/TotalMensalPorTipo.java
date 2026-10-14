package br.com.bassi.monify.repository;

import br.com.bassi.monify.domain.enums.TipoTransacao;

import java.math.BigDecimal;

public interface TotalMensalPorTipo {

    Integer getAno();

    Integer getMes();

    TipoTransacao getTipo();

    BigDecimal getTotal();
}
