package br.com.bassi.monify.repository;

import java.math.BigDecimal;
import java.util.UUID;

public interface GastoPorCategoria {

    UUID getCategoriaId();

    BigDecimal getTotal();
}
