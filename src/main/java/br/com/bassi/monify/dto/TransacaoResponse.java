package br.com.bassi.monify.dto;

import br.com.bassi.monify.domain.Categoria;
import br.com.bassi.monify.domain.Transacao;
import br.com.bassi.monify.domain.enums.TipoTransacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransacaoResponse(
        UUID id,
        CategoriaResumo categoria,
        TipoTransacao tipo,
        BigDecimal valor,
        String descricao,
        LocalDate dataOcorrencia) {

    public record CategoriaResumo(UUID id, String nome, String cor) {

        static CategoriaResumo from(Categoria categoria) {
            return new CategoriaResumo(categoria.getId(), categoria.getNome(), categoria.getCor());
        }
    }

    public static TransacaoResponse from(Transacao transacao) {
        return new TransacaoResponse(
                transacao.getId(),
                CategoriaResumo.from(transacao.getCategoria()),
                transacao.getTipo(),
                transacao.getValor(),
                transacao.getDescricao(),
                transacao.getDataOcorrencia());
    }
}
