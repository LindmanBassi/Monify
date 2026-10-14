package br.com.bassi.monify.dto;

import br.com.bassi.monify.domain.Aporte;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AporteResponse(UUID id, BigDecimal valor, LocalDate data, String observacao) {

    public static AporteResponse from(Aporte aporte) {
        return new AporteResponse(aporte.getId(), aporte.getValor(), aporte.getData(), aporte.getObservacao());
    }
}
