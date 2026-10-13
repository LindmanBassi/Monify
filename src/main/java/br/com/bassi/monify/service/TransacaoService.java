package br.com.bassi.monify.service;

import br.com.bassi.monify.domain.Categoria;
import br.com.bassi.monify.domain.Transacao;
import br.com.bassi.monify.dto.CriarTransacaoRequest;
import br.com.bassi.monify.dto.EditarTransacaoRequest;
import br.com.bassi.monify.dto.FiltroTransacoesRequest;
import br.com.bassi.monify.dto.PaginaResponse;
import br.com.bassi.monify.dto.TransacaoResponse;
import br.com.bassi.monify.exception.RecursoNaoEncontradoException;
import br.com.bassi.monify.repository.CategoriaRepository;
import br.com.bassi.monify.repository.TransacaoRepository;
import br.com.bassi.monify.repository.TransacaoSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransacaoService {

    // histórico sempre da data mais recente para a mais antiga
    private static final Sort ORDEM_HISTORICO = Sort.by(
            Sort.Order.desc("dataOcorrencia"),
            Sort.Order.desc("criadoEm"),
            Sort.Order.desc("id"));

    private final TransacaoRepository transacaoRepository;
    private final CategoriaRepository categoriaRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PaginaResponse<TransacaoResponse> listar(UUID usuarioId, FiltroTransacoesRequest filtro, Pageable pageable) {

        var paginacao = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ORDEM_HISTORICO);
        var especificacao = TransacaoSpecifications.doUsuarioComFiltros(usuarioId, filtro);

        return PaginaResponse.from(transacaoRepository.findAll(especificacao, paginacao).map(TransacaoResponse::from));
    }

    @Transactional(readOnly = true)
    public TransacaoResponse buscar(UUID usuarioId, UUID id) {

        return TransacaoResponse.from(buscarDoUsuario(usuarioId, id));
    }

    @Transactional
    public TransacaoResponse criar(UUID usuarioId, CriarTransacaoRequest request) {

        var categoria = buscarCategoriaDoUsuario(usuarioId, request.categoriaId());
        var transacao = new Transacao(usuarioId, categoria, request.valor(), request.descricao(),
                request.dataOcorrencia(), hoje());

        return TransacaoResponse.from(transacaoRepository.save(transacao));
    }

    @Transactional
    public void editar(UUID usuarioId, UUID id, EditarTransacaoRequest request) {

        var transacao = buscarDoUsuario(usuarioId, id);
        var categoria = buscarCategoriaDoUsuario(usuarioId, request.categoriaId());

        transacao.atualizar(categoria, request.valor(), request.descricao(), request.dataOcorrencia(), hoje());
    }

    // exclusão é definitiva
    @Transactional
    public void excluir(UUID usuarioId, UUID id) {

        transacaoRepository.delete(buscarDoUsuario(usuarioId, id));
    }

    // recurso de outro usuário responde como inexistente
    private Transacao buscarDoUsuario(UUID usuarioId, UUID id) {
        return transacaoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Transação não encontrada"));
    }

    private Categoria buscarCategoriaDoUsuario(UUID usuarioId, UUID categoriaId) {
        return categoriaRepository.findByIdAndUsuarioId(categoriaId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada"));
    }

    private LocalDate hoje() {
        return LocalDate.now(clock);
    }
}
