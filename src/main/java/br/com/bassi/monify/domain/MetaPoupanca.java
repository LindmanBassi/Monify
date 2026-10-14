package br.com.bassi.monify.domain;

import br.com.bassi.monify.domain.enums.StatusMetaPoupanca;
import br.com.bassi.monify.exception.RegraNegocioException;
import br.com.bassi.monify.util.Dinheiro;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

// Objetivo de juntar dinheiro, independente de categoria.
@Entity
@Table(name = "metas_poupanca")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MetaPoupanca extends EntidadeBase {

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "valor_alvo", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorAlvo;

    @Column(name = "prazo")
    private LocalDate prazo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private StatusMetaPoupanca status;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "metaPoupanca", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Aporte> aportes = new ArrayList<>();

    // prazo é opcional, mas não pode estar no passado
    public MetaPoupanca(UUID usuarioId, String nome, BigDecimal valorAlvo, LocalDate prazo, LocalDate hoje) {
        this.usuarioId = usuarioId;
        this.status = StatusMetaPoupanca.ATIVA;
        aplicarDados(nome, valorAlvo, prazo, hoje);
    }

    // só metas ativas são editadas; reduzir o alvo até o acumulado conclui a meta
    public void editar(String nome, BigDecimal valorAlvo, LocalDate prazo, LocalDate hoje) {

        if (status != StatusMetaPoupanca.ATIVA) {
            throw new RegraNegocioException("Somente metas de poupança ativas podem ser editadas");
        }

        aplicarDados(nome, valorAlvo, prazo, hoje);
        concluirSeAtingiuAlvo(calcularAcumulado());
    }

    // valor positivo = aporte, negativo = resgate; ao atingir o alvo a meta é concluída
    // concluída é um marco permanente: aceita apenas resgates e continua concluída
    public Aporte registrarAporte(BigDecimal valor, LocalDate data, String observacao, LocalDate hoje) {

        var valorNormalizado = Dinheiro.normalizar(valor, "Valor");

        if (valorNormalizado.signum() == 0) {
            throw new RegraNegocioException("Valor do aporte não pode ser zero");
        }
        if (status == StatusMetaPoupanca.CANCELADA) {
            throw new RegraNegocioException("Meta de poupança cancelada não aceita aportes ou resgates");
        }
        if (status == StatusMetaPoupanca.CONCLUIDA && valorNormalizado.signum() > 0) {
            throw new RegraNegocioException("Meta de poupança concluída aceita apenas resgates");
        }
        if (data.isAfter(hoje)) {
            throw new RegraNegocioException("Data do aporte não pode ser futura");
        }

        var novoAcumulado = calcularAcumulado().add(valorNormalizado);

        if (novoAcumulado.signum() < 0) {
            throw new RegraNegocioException("Resgate maior que o valor acumulado");
        }

        var aporte = new Aporte(this, valorNormalizado, data, observacao);
        aportes.add(aporte);
        concluirSeAtingiuAlvo(novoAcumulado);

        return aporte;
    }

    // cancelada vira somente leitura; o acumulado permanece como histórico
    public void cancelar() {

        if (status != StatusMetaPoupanca.ATIVA) {
            throw new RegraNegocioException("Somente metas de poupança ativas podem ser canceladas");
        }

        status = StatusMetaPoupanca.CANCELADA;
    }

    public BigDecimal calcularAcumulado() {
        return aportes.stream()
                .map(Aporte::getValor)
                .reduce(Dinheiro.ZERO, BigDecimal::add);
    }

    public BigDecimal calcularFaltante() {
        return calcularFaltante(calcularAcumulado());
    }

    // quanto falta para o alvo (nunca negativo)
    public BigDecimal calcularFaltante(BigDecimal acumulado) {
        return valorAlvo.subtract(acumulado).max(Dinheiro.ZERO);
    }

    public BigDecimal calcularPercentual(BigDecimal acumulado) {
        return Dinheiro.calcularPercentual(acumulado, valorAlvo);
    }

    public boolean isPrazoVencido(LocalDate hoje) {
        return status == StatusMetaPoupanca.ATIVA && prazo != null && prazo.isBefore(hoje);
    }

    // quanto guardar por mês para cumprir o prazo, contando do mês atual até o mês do prazo
    // nulo quando não há prazo, a meta não está ativa ou o prazo já venceu
    public BigDecimal calcularValorPorMes(BigDecimal acumulado, LocalDate hoje) {

        if (prazo == null || status != StatusMetaPoupanca.ATIVA || isPrazoVencido(hoje)) {
            return null;
        }

        var mesesRestantes = ChronoUnit.MONTHS.between(YearMonth.from(hoje), YearMonth.from(prazo)) + 1;
        return Dinheiro.dividirArredondandoParaCima(calcularFaltante(acumulado), mesesRestantes);
    }

    public List<Aporte> getAportes() {
        return Collections.unmodifiableList(aportes);
    }

    private void aplicarDados(String nome, BigDecimal valorAlvo, LocalDate prazo, LocalDate hoje) {

        if (prazo != null && prazo.isBefore(hoje)) {
            throw new RegraNegocioException("Prazo não pode estar no passado");
        }

        this.nome = nome.trim();
        this.valorAlvo = Dinheiro.validarPositivo(valorAlvo, "Valor alvo");
        this.prazo = prazo;
    }

    private void concluirSeAtingiuAlvo(BigDecimal acumulado) {
        if (status == StatusMetaPoupanca.ATIVA && acumulado.compareTo(valorAlvo) >= 0) {
            status = StatusMetaPoupanca.CONCLUIDA;
        }
    }
}
