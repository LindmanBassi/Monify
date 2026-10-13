package br.com.bassi.monify.controller;

import org.springframework.jdbc.core.JdbcTemplate;

final class BancoDeTeste {

    private static final String[] TABELAS = {
            "aportes",
            "metas_poupanca",
            "metas_economia",
            "orcamentos",
            "transacoes",
            "tokens_renovacao",
            "categorias",
            "usuarios"
    };

    private BancoDeTeste() {
    }

    static void limpar(JdbcTemplate jdbcTemplate) {
        for (var tabela : TABELAS) {
            jdbcTemplate.update("DELETE FROM " + tabela);
        }
    }
}
