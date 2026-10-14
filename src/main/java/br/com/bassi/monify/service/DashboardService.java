package br.com.bassi.monify.service;

import br.com.bassi.monify.domain.enums.TipoTransacao;
import br.com.bassi.monify.dto.CategoriaResumoResponse;
import br.com.bassi.monify.dto.DashboardResponse;
import br.com.bassi.monify.dto.ResumoMensalResponse;
import br.com.bassi.monify.dto.TotalCategoriaResponse;
import br.com.bassi.monify.dto.VariacaoMensalResponse;
import br.com.bassi.monify.repository.TotalMensalPorTipo;
import br.com.bassi.monify.repository.TotalPorCategoria;
import br.com.bassi.monify.repository.TransacaoRepository;
import br.com.bassi.monify.util.Dinheiro;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int MESES_DE_EVOLUCAO = 6;

    private final TransacaoRepository transacaoRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DashboardResponse gerar(UUID usuarioId, YearMonth mes) {

        var mesReferencia = mes != null ? mes : YearMonth.now(clock);

        // 1. evolução dos últimos meses; inclui o mês escolhido e o anterior
        var primeiroMes = mesReferencia.minusMonths(MESES_DE_EVOLUCAO - 1);
        var evolucao = montarEvolucao(transacaoRepository.somarPorMesETipo(
                usuarioId, primeiroMes.atDay(1), mesReferencia.atEndOfMonth()), primeiroMes);

        var resumo = evolucao.getLast();
        var resumoAnterior = evolucao.get(evolucao.size() - 2);

        // 2. totais do mês por categoria, separados em despesas e receitas
        var porCategoria = transacaoRepository.somarPorCategoria(usuarioId, mesReferencia.atDay(1), mesReferencia.atEndOfMonth());

        return new DashboardResponse(
                mesReferencia,
                resumo,
                compararComMesAnterior(resumo, resumoAnterior),
                totaisDoTipo(porCategoria, TipoTransacao.DESPESA, resumo.despesas()),
                totaisDoTipo(porCategoria, TipoTransacao.RECEITA, resumo.receitas()),
                evolucao);
    }

    // meses sem movimento entram com zero
    private List<ResumoMensalResponse> montarEvolucao(List<TotalMensalPorTipo> totais, YearMonth primeiroMes) {

        Map<YearMonth, Map<TipoTransacao, BigDecimal>> porMes = totais.stream()
                .collect(Collectors.groupingBy(
                        t -> YearMonth.of(t.getAno(), t.getMes()),
                        Collectors.toMap(TotalMensalPorTipo::getTipo, TotalMensalPorTipo::getTotal)));

        return Stream.iterate(primeiroMes, m -> m.plusMonths(1))
                .limit(MESES_DE_EVOLUCAO)
                .map(m -> {
                    var doMes = porMes.getOrDefault(m, Map.of());
                    return ResumoMensalResponse.of(m,
                            doMes.getOrDefault(TipoTransacao.RECEITA, Dinheiro.ZERO),
                            doMes.getOrDefault(TipoTransacao.DESPESA, Dinheiro.ZERO));
                })
                .toList();
    }

    private VariacaoMensalResponse compararComMesAnterior(ResumoMensalResponse atual, ResumoMensalResponse anterior) {
        return new VariacaoMensalResponse(
                calcularVariacao(atual.receitas(), anterior.receitas()),
                calcularVariacao(atual.despesas(), anterior.despesas()),
                calcularVariacao(atual.saldo(), anterior.saldo()));
    }

    // sem base de comparação quando o mês anterior é zero; sobre o valor absoluto para saldos negativos
    private BigDecimal calcularVariacao(BigDecimal atual, BigDecimal anterior) {
        if (anterior.signum() == 0) {
            return null;
        }
        return Dinheiro.calcularPercentual(atual.subtract(anterior), anterior.abs());
    }

    private List<TotalCategoriaResponse> totaisDoTipo(List<TotalPorCategoria> totais, TipoTransacao tipo, BigDecimal totalDoTipo) {
        return totais.stream()
                .filter(t -> t.getCategoria().getTipo() == tipo)
                .map(t -> new TotalCategoriaResponse(
                        CategoriaResumoResponse.from(t.getCategoria()),
                        t.getTotal(),
                        Dinheiro.calcularPercentual(t.getTotal(), totalDoTipo)))
                .toList();
    }
}
