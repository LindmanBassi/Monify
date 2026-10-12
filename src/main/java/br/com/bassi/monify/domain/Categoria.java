package br.com.bassi.monify.domain;

import br.com.bassi.monify.domain.enums.TipoTransacao;
import br.com.bassi.monify.exception.RegraNegocioException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "categorias",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_categorias_usuario_nome_tipo",
                columnNames = {"usuario_id", "nome", "tipo"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Categoria extends EntidadeBase {

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(name = "nome", nullable = false, length = 50)
    private String nome;

    // tipo fixo: não pode ser alterado após a criação
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, updatable = false, length = 10)
    private TipoTransacao tipo;

    @Column(name = "cor", nullable = false, length = 7)
    private String cor;

    @Column(name = "arquivada", nullable = false)
    private boolean arquivada;

    public Categoria(UUID usuarioId, String nome, TipoTransacao tipo, String cor) {
        this.usuarioId = usuarioId;
        this.nome = nome.trim();
        this.tipo = tipo;
        this.cor = cor;
    }

    public void editar(String nome, String cor) {
        this.nome = nome.trim();
        this.cor = cor;
    }

    // remover = arquivar
    public void arquivar() {
        this.arquivada = true;
    }

    public boolean isDespesa() {
        return tipo == TipoTransacao.DESPESA;
    }

    public boolean pertenceA(UUID usuarioId) {
        return this.usuarioId.equals(usuarioId);
    }

    // categoria do usuário e ativa: pode receber transações, orçamentos e metas
    public void validarAtivaDoUsuario(UUID usuarioId) {
        if (!pertenceA(usuarioId)) {
            throw new RegraNegocioException("Categoria não pertence ao usuário");
        }
        if (arquivada) {
            throw new RegraNegocioException("Categoria arquivada não pode ser utilizada");
        }
    }
}
