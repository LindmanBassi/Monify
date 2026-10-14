package br.com.bassi.monify.service;

import br.com.bassi.monify.domain.Categoria;
import br.com.bassi.monify.domain.MetaEconomia;
import br.com.bassi.monify.domain.Orcamento;
import br.com.bassi.monify.domain.enums.TipoReferencia;
import br.com.bassi.monify.dto.CriarMetaEconomiaRequest;
import br.com.bassi.monify.dto.EditarMetaEconomiaRequest;
import br.com.bassi.monify.dto.MetaEconomiaResponse;
import br.com.bassi.monify.exception.ConflitoException;
import br.com.bassi.monify.exception.RecursoNaoEncontradoException;
import br.com.bassi.monify.exception.RegraNegocioException;
import br.com.bassi.monify.repository.CategoriaRepository;
import br.com.bassi.monify.repository.GastoPorCategoria;
import br.com.bassi.monify.repository.MetaEconomiaRepository;
import br.com.bassi.monify.repository.OrcamentoRepository;
import br.com.bassi.monify.repository.TransacaoRepository;
import br.com.bassi.monify.util.Dinheiro;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MetaEconomiaService {

    private static final BigDecimal TRES_MESES = new BigDecimal("3");

    private final MetaEconomiaRepository metaEconomiaRepository;
    private final CategoriaRepository categoriaRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final TransacaoRepository transacaoRepository;
    private final Clock clock;

    // congelamento ao acessar: toda consulta congela antes as metas cujo mês já começou
    @Transactional
    public List<MetaEconomiaResponse> listar(UUID usuarioId, YearMonth mes) {

        congelarIniciadas(usuarioId);

        var mesReferencia = mes != null ? mes : mesAtual();
        var gastos = transacaoRepository.somarDespesasPorCategoria(usuarioId, mesReferencia.atDay(1), mesReferencia.atEndOfMonth())
                .stream()
                .collect(Collectors.toMap(GastoPorCategoria::getCategoriaId, GastoPorCategoria::getTotal));

        return metaEconomiaRepository.listarDoMes(usuarioId, mesReferencia)
                .stream()
                .map(meta -> MetaEconomiaResponse.from(meta,
                        gastos.getOrDefault(meta.getCategoria().getId(), Dinheiro.ZERO), mesAtual()))
                .toList();
    }

    @Transactional
    public MetaEconomiaResponse buscar(UUID usuarioId, UUID id) {

        congelarIniciadas(usuarioId);
        return paraResponse(buscarDoUsuario(usuarioId, id));
    }

    @Transactional
    public MetaEconomiaResponse criar(UUID usuarioId, CriarMetaEconomiaRequest request) {

        // 1. validar categoria do usuário e unicidade da meta ativa no mês
        var categoria = buscarCategoriaDoUsuario(usuarioId, request.categoriaId());
        if (metaEconomiaRepository.existsByUsuarioIdAndCategoriaIdAndMesReferenciaAndCanceladaEmIsNull(
                usuarioId, categoria.getId(), request.mesReferencia())) {
            throw new ConflitoException("Já existe uma meta de economia ativa para a categoria no mês");
        }

        // 2. validar a disponibilidade da referência escolhida
        var orcamento = buscarOrcamentoDaReferencia(usuarioId, categoria, request.mesReferencia(), request.tipoReferencia());
        validarHistoricoSuficiente(usuarioId, request.mesReferencia(), request.tipoReferencia());

        var meta = new MetaEconomia(usuarioId, categoria, request.mesReferencia(), request.tipoReferencia(),
                orcamento, request.valorEconomia(), mesAtual());

        // 3. meta do mês atual é congelada imediatamente; limite inválido bloqueia a criação
        if (meta.deveCongelar(mesAtual())) {
            meta.congelar(calcularReferencia(meta), agora());
        }

        return paraResponse(metaEconomiaRepository.save(meta));
    }

    // edição livre só antes do congelamento
    @Transactional
    public void editar(UUID usuarioId, UUID id, EditarMetaEconomiaRequest request) {

        congelarIniciadas(usuarioId);

        var meta = buscarDoUsuario(usuarioId, id);
        var orcamento = buscarOrcamentoDaReferencia(usuarioId, meta.getCategoria(), meta.getMesReferencia(),
                request.tipoReferencia());
        validarHistoricoSuficiente(usuarioId, meta.getMesReferencia(), request.tipoReferencia());

        meta.editar(request.tipoReferencia(), orcamento, request.valorEconomia());
    }

    @Transactional
    public void cancelar(UUID usuarioId, UUID id) {

        congelarIniciadas(usuarioId);

        var meta = buscarDoUsuario(usuarioId, id);
        meta.cancelar(calcularGasto(meta), mesAtual(), agora());
    }

    // arquivar a categoria cancela as metas pendentes dela
    @Transactional
    public void cancelarPendentesDaCategoria(UUID usuarioId, UUID categoriaId) {

        congelarIniciadas(usuarioId);

        metaEconomiaRepository.findByUsuarioIdAndCategoriaIdAndCanceladaEmIsNull(usuarioId, categoriaId)
                .forEach(this::cancelarSePendente);
    }

    // excluir o orçamento cancela as metas pendentes vinculadas; as demais mantêm os valores congelados
    @Transactional
    public void cancelarPendentesDoOrcamento(UUID usuarioId, UUID orcamentoId) {

        congelarIniciadas(usuarioId);

        metaEconomiaRepository.findByUsuarioIdAndOrcamentoId(usuarioId, orcamentoId)
                .forEach(meta -> {
                    cancelarSePendente(meta);
                    meta.desvincularOrcamento();
                });
    }

    private void congelarIniciadas(UUID usuarioId) {
        metaEconomiaRepository.listarParaCongelar(usuarioId, mesAtual())
                .forEach(meta -> meta.congelarNoInicioDoMes(calcularReferencia(meta), agora()));
    }

    private void cancelarSePendente(MetaEconomia meta) {
        var gasto = calcularGasto(meta);
        if (meta.calcularStatus(gasto, mesAtual()).isPendente()) {
            meta.cancelar(gasto, mesAtual(), agora());
        }
    }

    // nulo quando a referência não pode mais ser calculada (orçamento excluído ou histórico insuficiente)
    private BigDecimal calcularReferencia(MetaEconomia meta) {

        var mes = meta.getMesReferencia();

        if (meta.getTipoReferencia() == TipoReferencia.ORCAMENTO) {
            return meta.getOrcamento() != null ? meta.getOrcamento().getValorLimite() : null;
        }
        if (!historicoSuficiente(meta.getUsuarioId(), mes, meta.getTipoReferencia())) {
            return null;
        }
        if (meta.getTipoReferencia() == TipoReferencia.MES_ANTERIOR) {
            return somarGastoDaCategoria(meta, mes.minusMonths(1), mes.minusMonths(1));
        }

        // meses do histórico sem gasto na categoria contam como zero
        return somarGastoDaCategoria(meta, mes.minusMonths(3), mes.minusMonths(1))
                .divide(TRES_MESES, Dinheiro.ESCALA, Dinheiro.ARREDONDAMENTO);
    }

    private BigDecimal somarGastoDaCategoria(MetaEconomia meta, YearMonth de, YearMonth ate) {
        var total = transacaoRepository.somarDaCategoria(meta.getUsuarioId(), meta.getCategoria().getId(),
                de.atDay(1), ate.atEndOfMonth());
        return Dinheiro.normalizar(total, "Gasto");
    }

    private BigDecimal calcularGasto(MetaEconomia meta) {
        return somarGastoDaCategoria(meta, meta.getMesReferencia(), meta.getMesReferencia());
    }

    private Orcamento buscarOrcamentoDaReferencia(UUID usuarioId, Categoria categoria, YearMonth mes,
                                                  TipoReferencia tipoReferencia) {
        if (tipoReferencia != TipoReferencia.ORCAMENTO) {
            return null;
        }
        return orcamentoRepository.findByUsuarioIdAndCategoriaIdAndMesReferencia(usuarioId, categoria.getId(), mes)
                .orElse(null);
    }

    private void validarHistoricoSuficiente(UUID usuarioId, YearMonth mes, TipoReferencia tipoReferencia) {
        if (!historicoSuficiente(usuarioId, mes, tipoReferencia)) {
            throw new RegraNegocioException("Referência indisponível: histórico de transações insuficiente");
        }
    }

    // início do histórico = mês da primeira transação do usuário
    private boolean historicoSuficiente(UUID usuarioId, YearMonth mes, TipoReferencia tipoReferencia) {
        var primeiraData = transacaoRepository.buscarPrimeiraDataOcorrencia(usuarioId);
        var inicioHistorico = primeiraData != null ? YearMonth.from(primeiraData) : null;
        return tipoReferencia.historicoSuficiente(mes, inicioHistorico);
    }

    private MetaEconomiaResponse paraResponse(MetaEconomia meta) {
        return MetaEconomiaResponse.from(meta, calcularGasto(meta), mesAtual());
    }

    // recurso de outro usuário responde como inexistente
    private MetaEconomia buscarDoUsuario(UUID usuarioId, UUID id) {
        return metaEconomiaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Meta de economia não encontrada"));
    }

    private Categoria buscarCategoriaDoUsuario(UUID usuarioId, UUID categoriaId) {
        return categoriaRepository.findByIdAndUsuarioId(categoriaId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada"));
    }

    private YearMonth mesAtual() {
        return YearMonth.now(clock);
    }

    private Instant agora() {
        return Instant.now(clock);
    }
}
