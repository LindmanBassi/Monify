package br.com.bassi.monify.dto;

import br.com.bassi.monify.domain.Transacao;
import br.com.bassi.monify.domain.enums.TipoTransacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransacaoResponse(
        UUID id,
        CategoriaResumoResponse categoria,
        TipoTransacao tipo,
        BigDecimal valor,
        String descricao,
        LocalDate dataOcorrencia) {

    public static TransacaoResponse from(Transacao transacao) {
        return new TransacaoResponse(
                transacao.getId(),
                CategoriaResumoResponse.from(transacao.getCategoria()),
                transacao.getTipo(),
                transacao.getValor(),
                transacao.getDescricao(),
                transacao.getDataOcorrencia());
    }
}
