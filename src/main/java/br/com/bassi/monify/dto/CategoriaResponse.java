package br.com.bassi.monify.dto;

import br.com.bassi.monify.domain.Categoria;
import br.com.bassi.monify.domain.enums.IconeCategoria;
import br.com.bassi.monify.domain.enums.TipoTransacao;

import java.util.UUID;

public record CategoriaResponse(UUID id, String nome, TipoTransacao tipo, String cor, IconeCategoria icone, boolean arquivada) {

    public static CategoriaResponse from(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNome(),
                categoria.getTipo(),
                categoria.getCor(),
                categoria.getIcone(),
                categoria.isArquivada());
    }
}
