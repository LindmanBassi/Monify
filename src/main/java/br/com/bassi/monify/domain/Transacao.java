package br.com.bassi.monify.domain;

import br.com.bassi.monify.domain.enums.TipoTransacao;
import br.com.bassi.monify.exception.RegraNegocioException;
import br.com.bassi.monify.util.Dinheiro;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transacoes",
        indexes = @Index(name = "idx_transacoes_usuario_data", columnList = "usuario_id, data_ocorrencia"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transacao extends EntidadeBase {

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 10)
    private TipoTransacao tipo;

    @Column(name = "valor", nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "data_ocorrencia", nullable = false)
    private LocalDate dataOcorrencia;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    public Transacao(UUID usuarioId, Categoria categoria, TipoTransacao tipo, BigDecimal valor,
                     String descricao, LocalDate dataOcorrencia) {
        this.usuarioId = usuarioId;
        aplicarDados(categoria, tipo, valor, descricao, dataOcorrencia);
    }

    // edição aplica as mesmas validações da criação
    public void atualizar(Categoria categoria, TipoTransacao tipo, BigDecimal valor,
                          String descricao, LocalDate dataOcorrencia) {
        aplicarDados(categoria, tipo, valor, descricao, dataOcorrencia);
    }

    private void aplicarDados(Categoria categoria, TipoTransacao tipo, BigDecimal valor,
                              String descricao, LocalDate dataOcorrencia) {

        categoria.validarAtivaDoUsuario(usuarioId);

        if (categoria.getTipo() != tipo) {
            throw new RegraNegocioException("Tipo da transação difere do tipo da categoria");
        }

        this.categoria = categoria;
        this.tipo = tipo;
        this.valor = Dinheiro.validarPositivo(valor, "Valor");
        this.descricao = descricao;
        this.dataOcorrencia = dataOcorrencia;
    }
}
