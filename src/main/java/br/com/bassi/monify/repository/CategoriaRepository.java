package br.com.bassi.monify.repository;

import br.com.bassi.monify.domain.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {

    List<Categoria> findAllByUsuarioId(UUID usuarioId);
}
