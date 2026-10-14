package br.com.bassi.monify.dto;

import java.math.BigDecimal;

public record TotalCategoriaResponse(CategoriaResumoResponse categoria, BigDecimal total, BigDecimal percentual) {
}
