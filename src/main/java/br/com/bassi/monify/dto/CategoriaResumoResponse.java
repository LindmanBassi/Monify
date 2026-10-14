package br.com.bassi.monify.dto;

import br.com.bassi.monify.domain.Categoria;

import java.util.UUID;

public record CategoriaResumoResponse(UUID id, String nome, String cor, boolean arquivada) {

    public static CategoriaResumoResponse from(Categoria categoria) {
        return new CategoriaResumoResponse(categoria.getId(), categoria.getNome(), categoria.getCor(), categoria.isArquivada());
    }
}
