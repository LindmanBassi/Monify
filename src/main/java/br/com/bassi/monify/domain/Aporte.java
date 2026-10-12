package br.com.bassi.monify.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

// Aporte (valor positivo) ou resgate (valor negativo) de uma meta de poupança.
@Entity
@Table(name = "aportes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Aporte extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meta_poupanca_id", nullable = false, updatable = false)
    private MetaPoupanca metaPoupanca;

    @Column(name = "valor", nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "observacao")
    private String observacao;

    Aporte(MetaPoupanca metaPoupanca, BigDecimal valor, LocalDate data, String observacao) {
        this.metaPoupanca = metaPoupanca;
        this.valor = valor;
        this.data = data;
        this.observacao = observacao;
    }
}
