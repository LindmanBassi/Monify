package br.com.bassi.monify.service;

import br.com.bassi.monify.domain.MetaPoupanca;
import br.com.bassi.monify.domain.enums.StatusMetaPoupanca;
import br.com.bassi.monify.dto.AporteResponse;
import br.com.bassi.monify.dto.CriarMetaPoupancaRequest;
import br.com.bassi.monify.dto.EditarMetaPoupancaRequest;
import br.com.bassi.monify.dto.MetaPoupancaResponse;
import br.com.bassi.monify.dto.PaginaResponse;
import br.com.bassi.monify.dto.RegistrarAporteRequest;
import br.com.bassi.monify.exception.RecursoNaoEncontradoException;
import br.com.bassi.monify.repository.AcumuladoPorMeta;
import br.com.bassi.monify.repository.AporteRepository;
import br.com.bassi.monify.repository.MetaPoupancaRepository;
import br.com.bassi.monify.util.Dinheiro;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MetaPoupancaService {

    // extrato do mais recente para o mais antigo
    private static final Sort ORDEM_EXTRATO = Sort.by(Sort.Order.desc("data"), Sort.Order.desc("id"));

    private final MetaPoupancaRepository metaPoupancaRepository;
    private final AporteRepository aporteRepository;
    private final Clock clock;

    // acumulado de todas as metas somado em uma única consulta
    @Transactional(readOnly = true)
    public List<MetaPoupancaResponse> listar(UUID usuarioId, StatusMetaPoupanca status) {

        var acumulados = aporteRepository.somarPorMeta(usuarioId)
                .stream()
                .collect(Collectors.toMap(AcumuladoPorMeta::getMetaPoupancaId, AcumuladoPorMeta::getTotal));

        return metaPoupancaRepository.listar(usuarioId, status)
                .stream()
                .map(meta -> MetaPoupancaResponse.from(meta, acumulados.getOrDefault(meta.getId(), Dinheiro.ZERO), hoje()))
                .toList();
    }

    @Transactional(readOnly = true)
    public MetaPoupancaResponse buscar(UUID usuarioId, UUID id) {

        return paraResponse(buscarDoUsuario(usuarioId, id));
    }

    @Transactional
    public MetaPoupancaResponse criar(UUID usuarioId, CriarMetaPoupancaRequest request) {

        var meta = metaPoupancaRepository.save(new MetaPoupanca(usuarioId, request.nome(), request.valorAlvo(),
                request.prazo(), hoje()));
        return MetaPoupancaResponse.from(meta, Dinheiro.ZERO, hoje());
    }

    @Transactional
    public void editar(UUID usuarioId, UUID id, EditarMetaPoupancaRequest request) {

        buscarDoUsuario(usuarioId, id).editar(request.nome(), request.valorAlvo(), request.prazo(), hoje());
    }

    // o acumulado permanece como histórico
    @Transactional
    public void cancelar(UUID usuarioId, UUID id) {

        buscarDoUsuario(usuarioId, id).cancelar();
    }

    // aportes são imutáveis: correções são feitas com um lançamento contrário
    @Transactional
    public AporteResponse registrarAporte(UUID usuarioId, UUID id, RegistrarAporteRequest request) {

        var meta = buscarDoUsuario(usuarioId, id);
        var aporte = meta.registrarAporte(request.valor(), request.data(), request.observacao(), hoje());

        return AporteResponse.from(aporteRepository.save(aporte));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<AporteResponse> listarAportes(UUID usuarioId, UUID id, Pageable pageable) {

        var meta = buscarDoUsuario(usuarioId, id);
        var paginacao = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ORDEM_EXTRATO);

        return PaginaResponse.from(aporteRepository.findByMetaPoupancaId(meta.getId(), paginacao).map(AporteResponse::from));
    }

    private MetaPoupancaResponse paraResponse(MetaPoupanca meta) {
        var acumulado = Dinheiro.normalizar(aporteRepository.somarDaMeta(meta.getId()), "Acumulado");
        return MetaPoupancaResponse.from(meta, acumulado, hoje());
    }

    // recurso de outro usuário responde como inexistente
    private MetaPoupanca buscarDoUsuario(UUID usuarioId, UUID id) {
        return metaPoupancaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Meta de poupança não encontrada"));
    }

    private LocalDate hoje() {
        return LocalDate.now(clock);
    }
}
