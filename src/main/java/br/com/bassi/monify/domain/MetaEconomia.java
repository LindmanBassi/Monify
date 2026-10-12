package br.com.bassi.monify.domain;

import br.com.bassi.monify.domain.enums.StatusMetaEconomia;
import br.com.bassi.monify.domain.enums.TipoReferencia;
import br.com.bassi.monify.exception.RegraNegocioException;
import br.com.bassi.monify.util.Dinheiro;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

// Meta de gastar menos em uma categoria de despesa em um mês, em relação a uma referência.
@Entity
@Table(name = "metas_economia")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MetaEconomia extends EntidadeBase {

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false, updatable = false)
    private Categoria categoria;

    // preenchido apenas quando a referência é ORCAMENTO
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orcamento_id")
    private Orcamento orcamento;

    @Column(name = "mes_referencia", nullable = false, updatable = false)
    private YearMonth mesReferencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_referencia", nullable = false, length = 20)
    private TipoReferencia tipoReferencia;

    @Column(name = "valor_economia", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorEconomia;

    @Column(name = "valor_referencia", precision = 19, scale = 2)
    private BigDecimal valorReferencia;

    @Column(name = "valor_limite", precision = 19, scale = 2)
    private BigDecimal valorLimite;

    @Column(name = "congelada_em")
    private Instant congeladaEm;

    @Column(name = "cancelada_em")
    private Instant canceladaEm;

    public MetaEconomia(UUID usuarioId, Categoria categoria, YearMonth mesReferencia,
                        TipoReferencia tipoReferencia, Orcamento orcamento,
                        BigDecimal valorEconomia, YearMonth mesAtual) {

        categoria.validarAtivaDoUsuario(usuarioId);

        if (!categoria.isDespesa()) {
            throw new RegraNegocioException("Meta de economia só pode ser criada para categorias de despesa");
        }
        if (mesReferencia.isBefore(mesAtual)) {
            throw new RegraNegocioException("Meta de economia não pode ser criada para mês passado");
        }

        this.usuarioId = usuarioId;
        this.categoria = categoria;
        this.mesReferencia = mesReferencia;
        definirReferencia(tipoReferencia, orcamento, valorEconomia);
    }

    // edição livre enquanto a meta não foi congelada nem cancelada
    public void editar(TipoReferencia tipoReferencia, Orcamento orcamento, BigDecimal valorEconomia) {

        validarNaoCancelada();

        if (isCongelada()) {
            throw new RegraNegocioException("Meta de economia congelada não pode ser editada");
        }

        definirReferencia(tipoReferencia, orcamento, valorEconomia);
    }

    // o mês da meta já começou e a referência ainda precisa ser congelada
    public boolean deveCongelar(YearMonth mesAtual) {
        return !isCancelada() && !isCongelada() && !mesReferencia.isAfter(mesAtual);
    }

    // congela a referência no início do mês; depois disso, mudanças no passado não afetam a meta
    public void congelar(BigDecimal valorReferencia, Instant agora) {

        validarNaoCancelada();

        if (isCongelada()) {
            throw new RegraNegocioException("Meta de economia já está congelada");
        }

        var referencia = Dinheiro.normalizar(valorReferencia, "Valor de referência");
        var limite = referencia.subtract(valorEconomia);

        // PENDENTE (CLAUDE.md §11): tratamento definitivo para limite <= 0. Por ora, rejeita.
        if (limite.signum() <= 0) {
            throw new RegraNegocioException("Valor de economia deve ser menor que o valor de referência");
        }

        this.valorReferencia = referencia;
        this.valorLimite = limite;
        this.congeladaEm = agora;
    }

    // cancelamento manual ou automático: só é permitido enquanto a meta estiver pendente
    public void cancelar(BigDecimal gastoNoMes, YearMonth mesAtual, Instant agora) {

        if (!calcularStatus(gastoNoMes, mesAtual).isPendente()) {
            throw new RegraNegocioException("Somente metas pendentes podem ser canceladas");
        }

        this.canceladaEm = agora;
    }

    public StatusMetaEconomia calcularStatus(BigDecimal gastoNoMes, YearMonth mesAtual) {

        if (isCancelada()) {
            return StatusMetaEconomia.CANCELADA;
        }
        if (!isCongelada()) {
            return StatusMetaEconomia.AGUARDANDO_INICIO;
        }
        if (gastoNoMes.compareTo(valorLimite) > 0) {
            return StatusMetaEconomia.NAO_ATINGIDA;
        }
        if (mesAtual.isAfter(mesReferencia)) {
            return StatusMetaEconomia.ATINGIDA;
        }
        return StatusMetaEconomia.EM_ANDAMENTO;
    }

    // economia realizada = valor de referência - gasto (disponível após o congelamento)
    public BigDecimal calcularEconomiaRealizada(BigDecimal gastoNoMes) {

        if (!isCongelada()) {
            throw new IllegalStateException("Meta de economia ainda não foi congelada");
        }

        return valorReferencia.subtract(gastoNoMes);
    }

    public boolean isCongelada() {
        return congeladaEm != null;
    }

    public boolean isCancelada() {
        return canceladaEm != null;
    }

    private void definirReferencia(TipoReferencia tipoReferencia, Orcamento orcamento, BigDecimal valorEconomia) {

        if (tipoReferencia == TipoReferencia.ORCAMENTO) {
            validarOrcamentoDaMeta(orcamento);
        } else if (orcamento != null) {
            throw new RegraNegocioException("Orçamento só deve ser informado para referência ORCAMENTO");
        }

        this.tipoReferencia = tipoReferencia;
        this.orcamento = orcamento;
        this.valorEconomia = Dinheiro.validarPositivo(valorEconomia, "Valor de economia");
    }

    private void validarOrcamentoDaMeta(Orcamento orcamento) {

        if (orcamento == null) {
            throw new RegraNegocioException("Não há orçamento para a categoria no mês da meta");
        }

        var mesmoUsuario = orcamento.getUsuarioId().equals(usuarioId);
        var mesmaCategoria = orcamento.getCategoria().equals(categoria);
        var mesmoMes = orcamento.getMesReferencia().equals(mesReferencia);

        if (!mesmoUsuario || !mesmaCategoria || !mesmoMes) {
            throw new RegraNegocioException("Orçamento não corresponde à categoria e ao mês da meta");
        }
    }

    private void validarNaoCancelada() {
        if (isCancelada()) {
            throw new RegraNegocioException("Meta de economia cancelada");
        }
    }
}
