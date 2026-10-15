package br.com.bassi.monify.domain.enums;

import br.com.bassi.monify.domain.Categoria;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

// copiadas para o usuário no cadastro
// PENDENTE: cores provisórias; a paleta final será definida junto com o frontend
public enum CategoriaPadrao {
    ALIMENTACAO("Alimentação", TipoTransacao.DESPESA, "#F97316", IconeCategoria.TALHERES),
    MORADIA("Moradia", TipoTransacao.DESPESA, "#8B5CF6", IconeCategoria.CASA),
    TRANSPORTE("Transporte", TipoTransacao.DESPESA, "#3B82F6", IconeCategoria.CARRO),
    SAUDE("Saúde", TipoTransacao.DESPESA, "#EF4444", IconeCategoria.CORACAO),
    EDUCACAO("Educação", TipoTransacao.DESPESA, "#14B8A6", IconeCategoria.CAPELO),
    LAZER("Lazer", TipoTransacao.DESPESA, "#EC4899", IconeCategoria.CLAQUETE),
    COMPRAS("Compras", TipoTransacao.DESPESA, "#EAB308", IconeCategoria.SACOLA),
    OUTROS_DESPESA("Outros", TipoTransacao.DESPESA, "#6B7280", IconeCategoria.FORMAS),
    SALARIO("Salário", TipoTransacao.RECEITA, "#22C55E", IconeCategoria.MALETA),
    INVESTIMENTOS("Investimentos", TipoTransacao.RECEITA, "#06B6D4", IconeCategoria.BANCO),
    OUTROS_RECEITA("Outros", TipoTransacao.RECEITA, "#84CC16", IconeCategoria.MOEDAS);

    private final String nome;
    private final TipoTransacao tipo;
    private final String cor;
    private final IconeCategoria icone;

    CategoriaPadrao(String nome, TipoTransacao tipo, String cor, IconeCategoria icone) {
        this.nome = nome;
        this.tipo = tipo;
        this.cor = cor;
        this.icone = icone;
    }

    public static List<Categoria> criarPara(UUID usuarioId) {
        return Arrays.stream(values())
                .map(padrao -> new Categoria(usuarioId, padrao.nome, padrao.tipo, padrao.cor, padrao.icone))
                .toList();
    }
}
