package br.com.bassi.monify.dto;

import java.math.BigDecimal;

// variação percentual em relação ao mês anterior; nula quando o mês anterior é zero (sem base de comparação)
public record VariacaoMensalResponse(BigDecimal receitas, BigDecimal despesas, BigDecimal saldo) {
}
