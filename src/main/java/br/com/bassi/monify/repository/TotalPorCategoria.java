package br.com.bassi.monify.repository;

import br.com.bassi.monify.domain.Categoria;

import java.math.BigDecimal;

public interface TotalPorCategoria {

    Categoria getCategoria();

    BigDecimal getTotal();
}
