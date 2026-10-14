package br.com.bassi.monify.dto;

import br.com.bassi.monify.domain.MetaPoupanca;
import br.com.bassi.monify.domain.enums.StatusMetaPoupanca;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record MetaPoupancaResponse(
        UUID id,
        String nome,
        BigDecimal valorAlvo,
        LocalDate prazo,
        StatusMetaPoupanca status,
        BigDecimal acumulado,
        BigDecimal percentual,
        BigDecimal faltante,
        BigDecimal valorPorMes,
        boolean prazoVencido) {

    // acumulado somado no banco; percentual, faltante e valor por mês calculados pela entidade
    public static MetaPoupancaResponse from(MetaPoupanca meta, BigDecimal acumulado, LocalDate hoje) {
        return new MetaPoupancaResponse(
                meta.getId(),
                meta.getNome(),
                meta.getValorAlvo(),
                meta.getPrazo(),
                meta.getStatus(),
                acumulado,
                meta.calcularPercentual(acumulado),
                meta.calcularFaltante(acumulado),
                meta.calcularValorPorMes(acumulado, hoje),
                meta.isPrazoVencido(hoje));
    }
}
