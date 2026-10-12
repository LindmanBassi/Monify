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

    // prazo é opcional
    public MetaPoupanca(UUID usuarioId, String nome, BigDecimal valorAlvo, LocalDate prazo) {
        this.usuarioId = usuarioId;
        this.nome = nome.trim();
        this.valorAlvo = Dinheiro.validarPositivo(valorAlvo, "Valor alvo");
        this.prazo = prazo;
        this.status = StatusMetaPoupanca.ATIVA;
    }

    // valor positivo = aporte, negativo = resgate; ao atingir o alvo a meta é concluída
    public Aporte registrarAporte(BigDecimal valor, LocalDate data, String observacao) {

        // PENDENTE (CLAUDE.md §11): comportamento de metas CONCLUIDA/CANCELADA. Por ora, só ATIVA aceita.
        if (status != StatusMetaPoupanca.ATIVA) {
            throw new RegraNegocioException("Somente metas de poupança ativas aceitam aportes ou resgates");
        }

        var valorNormalizado = Dinheiro.normalizar(valor, "Valor");

        if (valorNormalizado.signum() == 0) {
            throw new RegraNegocioException("Valor do aporte não pode ser zero");
        }

        var novoAcumulado = calcularAcumulado().add(valorNormalizado);

        if (novoAcumulado.signum() < 0) {
            throw new RegraNegocioException("Resgate maior que o valor acumulado");
        }

        var aporte = new Aporte(this, valorNormalizado, data, observacao);
        aportes.add(aporte);

        if (novoAcumulado.compareTo(valorAlvo) >= 0) {
            status = StatusMetaPoupanca.CONCLUIDA;
        }

        return aporte;
    }

    public BigDecimal calcularAcumulado() {
        return aportes.stream()
                .map(Aporte::getValor)
                .reduce(Dinheiro.ZERO, BigDecimal::add);
    }

    // quanto falta para o alvo (nunca negativo)
    public BigDecimal calcularFaltante() {
        return valorAlvo.subtract(calcularAcumulado()).max(Dinheiro.ZERO);
    }

    public List<Aporte> getAportes() {
        return Collections.unmodifiableList(aportes);
    }
}
