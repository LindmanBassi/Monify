package br.com.bassi.monify.service;

import br.com.bassi.monify.domain.Categoria;
import br.com.bassi.monify.domain.enums.TipoTransacao;
import br.com.bassi.monify.dto.CategoriaResponse;
import br.com.bassi.monify.dto.CriarCategoriaRequest;
import br.com.bassi.monify.dto.EditarCategoriaRequest;
import br.com.bassi.monify.exception.ConflitoException;
import br.com.bassi.monify.exception.RecursoNaoEncontradoException;
import br.com.bassi.monify.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    // arquivadas ficam fora da listagem, salvo quando pedidas
    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar(UUID usuarioId, TipoTransacao tipo, boolean incluirArquivadas) {

        return categoriaRepository.listar(usuarioId, tipo, incluirArquivadas)
                .stream()
                .map(CategoriaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse buscar(UUID usuarioId, UUID id) {

        return CategoriaResponse.from(buscarDoUsuario(usuarioId, id));
    }

    @Transactional
    public CategoriaResponse criar(UUID usuarioId, CriarCategoriaRequest request) {

        var nome = request.nome().trim();
        if (categoriaRepository.existsByUsuarioIdAndNomeAndTipo(usuarioId, nome, request.tipo())) {
            throw nomeDuplicado(request.tipo());
        }

        var categoria = categoriaRepository.save(new Categoria(usuarioId, nome, request.tipo(), request.cor()));
        return CategoriaResponse.from(categoria);
    }

    // tipo fixo: só nome e cor podem ser editados
    @Transactional
    public void editar(UUID usuarioId, UUID id, EditarCategoriaRequest request) {

        var categoria = buscarDoUsuario(usuarioId, id);
        var nome = request.nome().trim();

        if (categoriaRepository.existsByUsuarioIdAndNomeAndTipoAndIdNot(usuarioId, nome, categoria.getTipo(), id)) {
            throw nomeDuplicado(categoria.getTipo());
        }

        categoria.editar(nome, request.cor());
    }

    // remover = arquivar: o histórico permanece intacto
    @Transactional
    public void arquivar(UUID usuarioId, UUID id) {

        buscarDoUsuario(usuarioId, id).arquivar();
    }

    @Transactional
    public void desarquivar(UUID usuarioId, UUID id) {

        buscarDoUsuario(usuarioId, id).desarquivar();
    }

    // categoria de outro usuário responde como inexistente
    private Categoria buscarDoUsuario(UUID usuarioId, UUID id) {
        return categoriaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada"));
    }

    private ConflitoException nomeDuplicado(TipoTransacao tipo) {
        return new ConflitoException("Já existe uma categoria de " + tipo.name().toLowerCase(Locale.ROOT) + " com esse nome");
    }
}
