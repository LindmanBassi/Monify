package br.com.bassi.monify.repository;

import br.com.bassi.monify.domain.Categoria;
import br.com.bassi.monify.domain.enums.TipoTransacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {

    List<Categoria> findAllByUsuarioId(UUID usuarioId);

    Optional<Categoria> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    boolean existsByUsuarioIdAndNomeAndTipo(UUID usuarioId, String nome, TipoTransacao tipo);

    boolean existsByUsuarioIdAndNomeAndTipoAndIdNot(UUID usuarioId, String nome, TipoTransacao tipo, UUID id);

    @Query("""
            select c from Categoria c
             where c.usuarioId = :usuarioId
               and (:tipo is null or c.tipo = :tipo)
               and (:incluirArquivadas = true or c.arquivada = false)
             order by c.nome
            """)
    List<Categoria> listar(UUID usuarioId, TipoTransacao tipo, boolean incluirArquivadas);
}
