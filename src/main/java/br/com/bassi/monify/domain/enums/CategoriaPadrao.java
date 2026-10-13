package br.com.bassi.monify.domain.enums;

import br.com.bassi.monify.domain.Categoria;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

// copiadas para o usuário no cadastro
// PENDENTE: cores provisórias; a paleta final será definida junto com o frontend
public enum CategoriaPadrao {
    ALIMENTACAO("Alimentação", TipoTransacao.DESPESA, "#F97316"),
    MORADIA("Moradia", TipoTransacao.DESPESA, "#8B5CF6"),
    TRANSPORTE("Transporte", TipoTransacao.DESPESA, "#3B82F6"),
    SAUDE("Saúde", TipoTransacao.DESPESA, "#EF4444"),
    EDUCACAO("Educação", TipoTransacao.DESPESA, "#14B8A6"),
    LAZER("Lazer", TipoTransacao.DESPESA, "#EC4899"),
    COMPRAS("Compras", TipoTransacao.DESPESA, "#EAB308"),
    OUTROS_DESPESA("Outros", TipoTransacao.DESPESA, "#6B7280"),
    SALARIO("Salário", TipoTransacao.RECEITA, "#22C55E"),
    INVESTIMENTOS("Investimentos", TipoTransacao.RECEITA, "#06B6D4"),
    OUTROS_RECEITA("Outros", TipoTransacao.RECEITA, "#84CC16");

    private final String nome;
    private final TipoTransacao tipo;
    private final String cor;

    CategoriaPadrao(String nome, TipoTransacao tipo, String cor) {
        this.nome = nome;
        this.tipo = tipo;
        this.cor = cor;
    }

    public static List<Categoria> criarPara(UUID usuarioId) {
        return Arrays.stream(values())
                .map(padrao -> new Categoria(usuarioId, padrao.nome, padrao.tipo, padrao.cor))
                .toList();
    }
}
