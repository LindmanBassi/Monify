package br.com.bassi.monify.service;

import br.com.bassi.monify.domain.Categoria;
import br.com.bassi.monify.domain.Orcamento;
import br.com.bassi.monify.dto.AlterarLimiteOrcamentoRequest;
import br.com.bassi.monify.dto.DefinirOrcamentoRequest;
import br.com.bassi.monify.dto.OrcamentoResponse;
import br.com.bassi.monify.exception.RecursoNaoEncontradoException;
import br.com.bassi.monify.repository.CategoriaRepository;
import br.com.bassi.monify.repository.GastoPorCategoria;
import br.com.bassi.monify.repository.OrcamentoRepository;
import br.com.bassi.monify.repository.TransacaoRepository;
import br.com.bassi.monify.util.Dinheiro;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrcamentoService {

    private final OrcamentoRepository orcamentoRepository;
    private final CategoriaRepository categoriaRepository;
    private final TransacaoRepository transacaoRepository;
    private final MetaEconomiaService metaEconomiaService;
    private final Clock clock;

    public record OrcamentoDefinido(OrcamentoResponse orcamento, boolean criado) {
    }

    // gastos de todas as categorias do mês somados em uma única consulta
    @Transactional(readOnly = true)
    public List<OrcamentoResponse> listar(UUID usuarioId, YearMonth mes) {

        var mesReferencia = mes != null ? mes : mesAtual();
        var gastos = transacaoRepository.somarDespesasPorCategoria(usuarioId, mesReferencia.atDay(1), mesReferencia.atEndOfMonth())
                .stream()
                .collect(Collectors.toMap(GastoPorCategoria::getCategoriaId, GastoPorCategoria::getTotal));

        return orcamentoRepository.listarDoMes(usuarioId, mesReferencia)
                .stream()
                .map(o -> OrcamentoResponse.from(o, gastos.getOrDefault(o.getCategoria().getId(), Dinheiro.ZERO)))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrcamentoResponse buscar(UUID usuarioId, UUID id) {

        return paraResponse(buscarDoUsuario(usuarioId, id));
    }

    // um orçamento por categoria e mês: definir de novo atualiza o existente
    @Transactional
    public OrcamentoDefinido definir(UUID usuarioId, DefinirOrcamentoRequest request) {

        // 1. validar categoria ativa do usuário
        var categoria = buscarCategoriaDoUsuario(usuarioId, request.categoriaId());
        categoria.validarAtivaDoUsuario(usuarioId);

        // 2. atualizar o orçamento existente da categoria no mês
        var existente = orcamentoRepository.findByUsuarioIdAndCategoriaIdAndMesReferencia(
                usuarioId, categoria.getId(), request.mesReferencia());

        if (existente.isPresent()) {
            var orcamento = existente.get();
            orcamento.alterarLimite(request.valorLimite(), mesAtual());
            return new OrcamentoDefinido(paraResponse(orcamento), false);
        }

        // 3. ou criar um novo
        var orcamento = orcamentoRepository.save(new Orcamento(usuarioId, categoria, request.mesReferencia(),
                request.valorLimite(), mesAtual()));
        return new OrcamentoDefinido(paraResponse(orcamento), true);
    }

    @Transactional
    public void alterarLimite(UUID usuarioId, UUID id, AlterarLimiteOrcamentoRequest request) {

        buscarDoUsuario(usuarioId, id).alterarLimite(request.valorLimite(), mesAtual());
    }

    // excluir cancela as metas de economia pendentes vinculadas ao orçamento
    @Transactional
    public void excluir(UUID usuarioId, UUID id) {

        var orcamento = buscarDoUsuario(usuarioId, id);
        orcamento.validarMesNaoPassado(mesAtual());

        metaEconomiaService.cancelarPendentesDoOrcamento(usuarioId, id);
        orcamentoRepository.delete(orcamento);
    }

    private OrcamentoResponse paraResponse(Orcamento orcamento) {
        var mes = orcamento.getMesReferencia();
        BigDecimal gasto = transacaoRepository.somarDaCategoria(orcamento.getUsuarioId(), orcamento.getCategoria().getId(),
                mes.atDay(1), mes.atEndOfMonth());
        return OrcamentoResponse.from(orcamento, Dinheiro.normalizar(gasto, "Gasto"));
    }

    // recurso de outro usuário responde como inexistente
    private Orcamento buscarDoUsuario(UUID usuarioId, UUID id) {
        return orcamentoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Orçamento não encontrado"));
    }

    private Categoria buscarCategoriaDoUsuario(UUID usuarioId, UUID categoriaId) {
        return categoriaRepository.findByIdAndUsuarioId(categoriaId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada"));
    }

    private YearMonth mesAtual() {
        return YearMonth.now(clock);
    }
}
