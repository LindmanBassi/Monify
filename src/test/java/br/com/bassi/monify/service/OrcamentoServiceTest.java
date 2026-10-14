package br.com.bassi.monify.service;

import br.com.bassi.monify.domain.Categoria;
import br.com.bassi.monify.domain.Orcamento;
import br.com.bassi.monify.domain.enums.StatusOrcamento;
import br.com.bassi.monify.domain.enums.TipoTransacao;
import br.com.bassi.monify.dto.DefinirOrcamentoRequest;
import br.com.bassi.monify.exception.RecursoNaoEncontradoException;
import br.com.bassi.monify.exception.RegraNegocioException;
import br.com.bassi.monify.repository.CategoriaRepository;
import br.com.bassi.monify.repository.GastoPorCategoria;
import br.com.bassi.monify.repository.OrcamentoRepository;
import br.com.bassi.monify.repository.TransacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrcamentoServiceTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID CATEGORIA_ID = UUID.randomUUID();
    private static final UUID ORCAMENTO_ID = UUID.randomUUID();

    // 01:30 UTC de 01/11 ainda é 31/10 em São Paulo
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-01T01:30:00Z"), ZoneId.of("America/Sao_Paulo"));
    private static final YearMonth OUTUBRO = YearMonth.of(2026, 10);

    @Mock
    private OrcamentoRepository orcamentoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private TransacaoRepository transacaoRepository;

    @Mock
    private MetaEconomiaService metaEconomiaService;

    private OrcamentoService orcamentoService;

    private final Categoria mercado = new Categoria(USUARIO_ID, "Mercado", TipoTransacao.DESPESA, "#FF0000");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mercado, "id", CATEGORIA_ID);
        orcamentoService = new OrcamentoService(orcamentoRepository, categoriaRepository, transacaoRepository, metaEconomiaService, CLOCK);
    }

    private Orcamento orcamentoDeMil() {
        return new Orcamento(USUARIO_ID, mercado, OUTUBRO, new BigDecimal("1000"), OUTUBRO);
    }

    private void prepararGastoDaCategoria(String gasto) {
        when(transacaoRepository.somarDaCategoria(USUARIO_ID, CATEGORIA_ID, OUTUBRO.atDay(1), OUTUBRO.atEndOfMonth()))
                .thenReturn(new BigDecimal(gasto));
    }

    @Nested
    class listar {

        @Test
        @DisplayName("Deve usar o mês atual no fuso de São Paulo e o gasto agregado de cada categoria")
        void deveUsarMesAtualEGastoAgregado() {

            //Arrange
            var gasto = mock(GastoPorCategoria.class);
            when(gasto.getCategoriaId()).thenReturn(CATEGORIA_ID);
            when(gasto.getTotal()).thenReturn(new BigDecimal("850.00"));
            when(transacaoRepository.somarDespesasPorCategoria(USUARIO_ID, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                    .thenReturn(List.of(gasto));
            when(orcamentoRepository.listarDoMes(USUARIO_ID, OUTUBRO)).thenReturn(List.of(orcamentoDeMil()));

            //Act
            var orcamentos = orcamentoService.listar(USUARIO_ID, null);

            //Assert
            var orcamento = orcamentos.getFirst();
            assertEquals(new BigDecimal("850.00"), orcamento.gasto());
            assertEquals(new BigDecimal("150.00"), orcamento.restante());
            assertEquals(new BigDecimal("85.00"), orcamento.percentual());
            assertEquals(StatusOrcamento.PROXIMO_DO_LIMITE, orcamento.status());
        }

        @Test
        @DisplayName("Deve considerar gasto zero quando a categoria não tiver despesas no mês")
        void deveConsiderarGastoZeroSemDespesas() {

            //Arrange
            when(transacaoRepository.somarDespesasPorCategoria(any(), any(), any())).thenReturn(List.of());
            when(orcamentoRepository.listarDoMes(USUARIO_ID, OUTUBRO)).thenReturn(List.of(orcamentoDeMil()));

            //Act
            var orcamento = orcamentoService.listar(USUARIO_ID, OUTUBRO).getFirst();

            //Assert
            assertEquals(new BigDecimal("0.00"), orcamento.gasto());
            assertEquals(StatusOrcamento.DENTRO_DO_LIMITE, orcamento.status());
        }
    }

    @Nested
    class definir {

        private final DefinirOrcamentoRequest request = new DefinirOrcamentoRequest(CATEGORIA_ID, OUTUBRO, new BigDecimal("1000"));

        @Test
        @DisplayName("Deve criar o orçamento quando não houver um para a categoria no mês")
        void deveCriarQuandoNaoExistir() {

            //Arrange
            when(categoriaRepository.findByIdAndUsuarioId(CATEGORIA_ID, USUARIO_ID)).thenReturn(Optional.of(mercado));
            when(orcamentoRepository.findByUsuarioIdAndCategoriaIdAndMesReferencia(USUARIO_ID, CATEGORIA_ID, OUTUBRO))
                    .thenReturn(Optional.empty());
            when(orcamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            prepararGastoDaCategoria("0");

            //Act
            var resultado = orcamentoService.definir(USUARIO_ID, request);

            //Assert
            assertTrue(resultado.criado());
            assertEquals(new BigDecimal("1000.00"), resultado.orcamento().valorLimite());
        }

        @Test
        @DisplayName("Deve atualizar o limite quando já houver orçamento para a categoria no mês")
        void deveAtualizarQuandoJaExistir() {

            //Arrange
            var existente = new Orcamento(USUARIO_ID, mercado, OUTUBRO, new BigDecimal("500"), OUTUBRO);
            when(categoriaRepository.findByIdAndUsuarioId(CATEGORIA_ID, USUARIO_ID)).thenReturn(Optional.of(mercado));
            when(orcamentoRepository.findByUsuarioIdAndCategoriaIdAndMesReferencia(USUARIO_ID, CATEGORIA_ID, OUTUBRO))
                    .thenReturn(Optional.of(existente));
            prepararGastoDaCategoria("0");

            //Act
            var resultado = orcamentoService.definir(USUARIO_ID, request);

            //Assert
            assertFalse(resultado.criado());
            assertEquals(new BigDecimal("1000.00"), existente.getValorLimite());
            verify(orcamentoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando a categoria estiver arquivada, mesmo com orçamento existente")
        void deveLancarExcecaoQuandoCategoriaArquivada() {

            //Arrange
            mercado.arquivar();
            when(categoriaRepository.findByIdAndUsuarioId(CATEGORIA_ID, USUARIO_ID)).thenReturn(Optional.of(mercado));

            //Act & Assert
            assertThrows(RegraNegocioException.class, () -> orcamentoService.definir(USUARIO_ID, request));
            verifyNoInteractions(orcamentoRepository);
        }

        @Test
        @DisplayName("Deve lançar não encontrado quando a categoria não for do usuário")
        void deveLancarNaoEncontradoQuandoCategoriaDeOutroUsuario() {

            //Arrange
            when(categoriaRepository.findByIdAndUsuarioId(CATEGORIA_ID, USUARIO_ID)).thenReturn(Optional.empty());

            //Act & Assert
            assertThrows(RecursoNaoEncontradoException.class, () -> orcamentoService.definir(USUARIO_ID, request));
        }
    }

    @Nested
    class excluir {

        @Test
        @DisplayName("Deve excluir o orçamento do mês atual")
        void deveExcluirOrcamentoDoMesAtual() {

            //Arrange
            var orcamento = orcamentoDeMil();
            when(orcamentoRepository.findByIdAndUsuarioId(ORCAMENTO_ID, USUARIO_ID)).thenReturn(Optional.of(orcamento));

            //Act
            orcamentoService.excluir(USUARIO_ID, ORCAMENTO_ID);

            //Assert
            verify(metaEconomiaService).cancelarPendentesDoOrcamento(USUARIO_ID, ORCAMENTO_ID);
            verify(orcamentoRepository).delete(orcamento);
        }

        @Test
        @DisplayName("Deve lançar não encontrado quando o orçamento não for do usuário")
        void deveLancarNaoEncontradoQuandoOrcamentoDeOutroUsuario() {

            //Arrange
            when(orcamentoRepository.findByIdAndUsuarioId(ORCAMENTO_ID, USUARIO_ID)).thenReturn(Optional.empty());

            //Act & Assert
            var excecao = assertThrows(RecursoNaoEncontradoException.class,
                    () -> orcamentoService.excluir(USUARIO_ID, ORCAMENTO_ID));
            assertEquals("Orçamento não encontrado", excecao.getMessage());
        }
    }
}
