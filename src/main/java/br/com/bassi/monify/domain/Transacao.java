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

    public Transacao(UUID usuarioId, Categoria categoria, BigDecimal valor, String descricao,
                     LocalDate dataOcorrencia, LocalDate hoje) {
        this.usuarioId = usuarioId;
        definirCategoria(categoria);
        aplicarDados(valor, descricao, dataOcorrencia, hoje);
    }

    // edição aplica as mesmas validações da criação; mantida a categoria, ela pode estar arquivada
    public void atualizar(Categoria categoria, BigDecimal valor, String descricao,
                          LocalDate dataOcorrencia, LocalDate hoje) {
        if (!categoria.equals(this.categoria)) {
            definirCategoria(categoria);
        }
        aplicarDados(valor, descricao, dataOcorrencia, hoje);
    }

    // tipo da transação é sempre o tipo da categoria
    private void definirCategoria(Categoria categoria) {
        categoria.validarAtivaDoUsuario(usuarioId);
        this.categoria = categoria;
        this.tipo = categoria.getTipo();
    }

    private void aplicarDados(BigDecimal valor, String descricao, LocalDate dataOcorrencia, LocalDate hoje) {

        if (dataOcorrencia.isAfter(hoje)) {
            throw new RegraNegocioException("Data da transação não pode ser futura");
        }

        this.valor = Dinheiro.validarPositivo(valor, "Valor");
        this.descricao = descricao == null || descricao.isBlank() ? null : descricao.trim();
        this.dataOcorrencia = dataOcorrencia;
    }
}
