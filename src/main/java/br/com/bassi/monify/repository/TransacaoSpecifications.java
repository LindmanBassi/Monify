package br.com.bassi.monify.repository;

import br.com.bassi.monify.domain.Transacao;
import br.com.bassi.monify.domain.enums.TipoTransacao;
import br.com.bassi.monify.dto.FiltroTransacoesRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

public final class TransacaoSpecifications {

    private TransacaoSpecifications() {
    }

    // filtros combináveis, sempre restritos ao dono
    public static Specification<Transacao> doUsuarioComFiltros(UUID usuarioId, FiltroTransacoesRequest filtro) {

        var especificacoes = new ArrayList<Specification<Transacao>>();
        especificacoes.add(doUsuario(usuarioId));

        if (filtro.inicio() != null) {
            especificacoes.add(aPartirDe(filtro.inicio()));
        }
        if (filtro.fim() != null) {
            especificacoes.add(ate(filtro.fim()));
        }
        if (filtro.categoriaId() != null) {
            especificacoes.add(daCategoria(filtro.categoriaId()));
        }
        if (filtro.tipo() != null) {
            especificacoes.add(doTipo(filtro.tipo()));
        }
        if (filtro.texto() != null && !filtro.texto().isBlank()) {
            especificacoes.add(comDescricaoContendo(filtro.texto().trim()));
        }

        return Specification.allOf(especificacoes);
    }

    private static Specification<Transacao> doUsuario(UUID usuarioId) {
        return (root, query, cb) -> cb.equal(root.get("usuarioId"), usuarioId);
    }

    private static Specification<Transacao> aPartirDe(LocalDate inicio) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dataOcorrencia"), inicio);
    }

    private static Specification<Transacao> ate(LocalDate fim) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("dataOcorrencia"), fim);
    }

    private static Specification<Transacao> daCategoria(UUID categoriaId) {
        return (root, query, cb) -> cb.equal(root.get("categoria").get("id"), categoriaId);
    }

    private static Specification<Transacao> doTipo(TipoTransacao tipo) {
        return (root, query, cb) -> cb.equal(root.get("tipo"), tipo);
    }

    private static Specification<Transacao> comDescricaoContendo(String texto) {
        var padrao = "%" + escaparCuringas(texto.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("descricao")), padrao, '\\');
    }

    private static String escaparCuringas(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
