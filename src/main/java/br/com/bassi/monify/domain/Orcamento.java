package br.com.bassi.monify.domain;

import br.com.bassi.monify.domain.enums.StatusOrcamento;
import br.com.bassi.monify.exception.RegraNegocioException;
import br.com.bassi.monify.util.Dinheiro;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

@Entity
@Table(name = "orcamentos",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_orcamentos_usuario_categoria_mes",
                columnNames = {"usuario_id", "categoria_id", "mes_referencia"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Orcamento extends EntidadeBase {

    private static final BigDecimal PERCENTUAL_ALERTA = new BigDecimal("80");
    private static final BigDecimal CEM = new BigDecimal("100");

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false, updatable = false)
    private Categoria categoria;

    @Column(name = "mes_referencia", nullable = false, updatable = false)
    private YearMonth mesReferencia;

    @Column(name = "valor_limite", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorLimite;

    public Orcamento(UUID usuarioId, Categoria categoria, YearMonth mesReferencia, BigDecimal valorLimite,
                     YearMonth mesAtual) {

        categoria.validarAtivaDoUsuario(usuarioId);
        if (!categoria.isDespesa()) {
            throw new RegraNegocioException("Orçamento só pode ser definido para categorias de despesa");
        }

        this.usuarioId = usuarioId;
        this.categoria = categoria;
        this.mesReferencia = mesReferencia;
        validarMesNaoPassado(mesAtual);
        this.valorLimite = Dinheiro.validarPositivo(valorLimite, "Valor limite");
    }

    // permitido mesmo se uma meta de economia usa este orçamento: o valor só é lido no congelamento
    public void alterarLimite(BigDecimal valorLimite, YearMonth mesAtual) {
        validarMesNaoPassado(mesAtual);
        this.valorLimite = Dinheiro.validarPositivo(valorLimite, "Valor limite");
    }

    // orçamentos de meses passados ficam como histórico: não são criados, alterados nem excluídos
    public void validarMesNaoPassado(YearMonth mesAtual) {
        if (mesReferencia.isBefore(mesAtual)) {
            throw new RegraNegocioException("Orçamento de mês passado não pode ser alterado");
        }
    }

    public StatusOrcamento calcularStatus(BigDecimal gasto) {

        var gastoX100 = gasto.multiply(CEM);

        if (gastoX100.compareTo(valorLimite.multiply(PERCENTUAL_ALERTA)) <= 0) {
            return StatusOrcamento.DENTRO_DO_LIMITE;
        }
        if (gasto.compareTo(valorLimite) <= 0) {
            return StatusOrcamento.PROXIMO_DO_LIMITE;
        }
        return StatusOrcamento.ESTOURADO;
    }

    // negativo quando estourado: indica quanto passou do limite
    public BigDecimal calcularRestante(BigDecimal gasto) {
        return valorLimite.subtract(gasto);
    }

    public BigDecimal calcularPercentual(BigDecimal gasto) {
        return Dinheiro.calcularPercentual(gasto, valorLimite);
    }
}
