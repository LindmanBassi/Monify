package br.com.bassi.monify.service;

import br.com.bassi.monify.domain.Categoria;
import br.com.bassi.monify.domain.MetaEconomia;
import br.com.bassi.monify.domain.enums.StatusMetaEconomia;
import br.com.bassi.monify.domain.enums.TipoReferencia;
import br.com.bassi.monify.domain.enums.TipoTransacao;
import br.com.bassi.monify.dto.CriarMetaEconomiaRequest;
import br.com.bassi.monify.exception.ConflitoException;
import br.com.bassi.monify.exception.RegraNegocioException;
import br.com.bassi.monify.repository.CategoriaRepository;
import br.com.bassi.monify.repository.MetaEconomiaRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetaEconomiaServiceTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID CATEGORIA_ID = UUID.randomUUID();
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private static final YearMonth JULHO = YearMonth.of(2026, 7);
    private static final YearMonth SETEMBRO = YearMonth.of(2026, 9);
    private static final YearMonth OUTUBRO = YearMonth.of(2026, 10);
    private static final YearMonth NOVEMBRO = YearMonth.of(2026, 11);

    @Mock
    private MetaEconomiaRepository metaEconomiaRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private OrcamentoRepository orcamentoRepository;

    @Mock
    private TransacaoRepository transacaoRepository;

    private MetaEconomiaService metaEconomiaService;

    private final Categoria mercado = new Categoria(USUARIO_ID, "Mercado", TipoTransacao.DESPESA, "#FF0000");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mercado, "id", CATEGORIA_ID);
        metaEconomiaService = new MetaEconomiaService(metaEconomiaRepository, categoriaRepository,
                orcamentoRepository, transacaoRepository, CLOCK);
    }

    private void prepararHistoricoDesde(YearMonth mes) {
        when(transacaoRepository.buscarPrimeiraDataOcorrencia(USUARIO_ID)).thenReturn(mes.atDay(1));
    }

    private void prepararGasto(YearMonth de, YearMonth ate, String valor) {
        when(transacaoRepository.somarDaCategoria(USUARIO_ID, CATEGORIA_ID, de.atDay(1), ate.atEndOfMonth()))
                .thenReturn(new BigDecimal(valor));
    }

    private void prepararCriacao() {
        when(categoriaRepository.findByIdAndUsuarioId(CATEGORIA_ID, USUARIO_ID)).thenReturn(Optional.of(mercado));
        when(metaEconomiaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    class criar {

        @Test
        @DisplayName("Deve congelar imediatamente a meta do mês atual com o gasto do mês anterior")
        void deveCongelarMetaDoMesAtualComMesAnterior() {

            //Arrange
            prepararCriacao();
            prepararHistoricoDesde(SETEMBRO);
            prepararGasto(SETEMBRO, SETEMBRO, "1000");
            prepararGasto(OUTUBRO, OUTUBRO, "300");

            //Act
            var meta = metaEconomiaService.criar(USUARIO_ID,
                    new CriarMetaEconomiaRequest(CATEGORIA_ID, OUTUBRO, TipoReferencia.MES_ANTERIOR, new BigDecimal("200")));

            //Assert
            assertEquals(new BigDecimal("1000.00"), meta.valorReferencia());
            assertEquals(new BigDecimal("800.00"), meta.valorLimite());
            assertEquals(new BigDecimal("700.00"), meta.economiaRealizada());
            assertEquals(StatusMetaEconomia.EM_ANDAMENTO, meta.status());
        }

        @Test
        @DisplayName("Deve usar a média dos três meses anteriores com arredondamento HALF_EVEN")
        void deveUsarMediaDosTresMeses() {

            //Arrange
            prepararCriacao();
            prepararHistoricoDesde(JULHO);
            prepararGasto(JULHO, SETEMBRO, "1000");
            prepararGasto(OUTUBRO, OUTUBRO, "0");

            //Act
            var meta = metaEconomiaService.criar(USUARIO_ID,
                    new CriarMetaEconomiaRequest(CATEGORIA_ID, OUTUBRO, TipoReferencia.MEDIA_TRES_MESES, new BigDecimal("100")));

            //Assert
            assertEquals(new BigDecimal("333.33"), meta.valorReferencia());
            assertEquals(new BigDecimal("233.33"), meta.valorLimite());
        }

        @Test
        @DisplayName("Deve lançar exceção quando o histórico não cobrir os três meses anteriores")
        void deveLancarExcecaoQuandoHistoricoInsuficiente() {

            //Arrange
            when(categoriaRepository.findByIdAndUsuarioId(CATEGORIA_ID, USUARIO_ID)).thenReturn(Optional.of(mercado));
            prepararHistoricoDesde(SETEMBRO);

            //Act & Assert
            var excecao = assertThrows(RegraNegocioException.class, () -> metaEconomiaService.criar(USUARIO_ID,
                    new CriarMetaEconomiaRequest(CATEGORIA_ID, OUTUBRO, TipoReferencia.MEDIA_TRES_MESES, BigDecimal.TEN)));
            assertEquals("Referência indisponível: histórico de transações insuficiente", excecao.getMessage());
            verify(metaEconomiaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando a economia não deixar limite positivo no mês atual")
        void deveLancarExcecaoQuandoLimiteInvalidoNoMesAtual() {

            //Arrange
            when(categoriaRepository.findByIdAndUsuarioId(CATEGORIA_ID, USUARIO_ID)).thenReturn(Optional.of(mercado));
            prepararHistoricoDesde(SETEMBRO);
            prepararGasto(SETEMBRO, SETEMBRO, "500");

            //Act & Assert
            assertThrows(RegraNegocioException.class, () -> metaEconomiaService.criar(USUARIO_ID,
                    new CriarMetaEconomiaRequest(CATEGORIA_ID, OUTUBRO, TipoReferencia.MES_ANTERIOR, new BigDecimal("500"))));
            verify(metaEconomiaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve criar meta de mês futuro aguardando o início, sem congelar")
        void deveCriarMetaFuturaSemCongelar() {

            //Arrange
            prepararCriacao();
            prepararHistoricoDesde(SETEMBRO);
            prepararGasto(NOVEMBRO, NOVEMBRO, "0");

            //Act
            var meta = metaEconomiaService.criar(USUARIO_ID,
                    new CriarMetaEconomiaRequest(CATEGORIA_ID, NOVEMBRO, TipoReferencia.MES_ANTERIOR, BigDecimal.TEN));

            //Assert
            assertNull(meta.valorReferencia());
            assertNull(meta.economiaRealizada());
            assertEquals(StatusMetaEconomia.AGUARDANDO_INICIO, meta.status());
        }

        @Test
        @DisplayName("Deve lançar conflito quando já houver meta ativa para a categoria no mês")
        void deveLancarConflitoQuandoMetaAtivaExistente() {

            //Arrange
            when(categoriaRepository.findByIdAndUsuarioId(CATEGORIA_ID, USUARIO_ID)).thenReturn(Optional.of(mercado));
            when(metaEconomiaRepository.existsByUsuarioIdAndCategoriaIdAndMesReferenciaAndCanceladaEmIsNull(
                    USUARIO_ID, CATEGORIA_ID, OUTUBRO)).thenReturn(true);

            //Act & Assert
            assertThrows(ConflitoException.class, () -> metaEconomiaService.criar(USUARIO_ID,
                    new CriarMetaEconomiaRequest(CATEGORIA_ID, OUTUBRO, TipoReferencia.MES_ANTERIOR, BigDecimal.TEN)));
        }
    }

    @Nested
    class listar {

        @Test
        @DisplayName("Deve congelar as metas iniciadas antes de listar")
        void deveCongelarMetasIniciadas() {

            //Arrange
            var meta = new MetaEconomia(USUARIO_ID, mercado, OUTUBRO, TipoReferencia.MES_ANTERIOR, null,
                    new BigDecimal("200"), SETEMBRO);
            when(metaEconomiaRepository.listarParaCongelar(USUARIO_ID, OUTUBRO)).thenReturn(List.of(meta));
            prepararHistoricoDesde(SETEMBRO);
            prepararGasto(SETEMBRO, SETEMBRO, "1000");

            //Act
            metaEconomiaService.listar(USUARIO_ID, OUTUBRO);

            //Assert
            assertTrue(meta.isCongelada());
            assertEquals(new BigDecimal("800.00"), meta.getValorLimite());
        }

        @Test
        @DisplayName("Deve cancelar a meta iniciada quando a referência deixar de estar disponível")
        void deveCancelarMetaQuandoReferenciaIndisponivel() {

            //Arrange
            var meta = new MetaEconomia(USUARIO_ID, mercado, OUTUBRO, TipoReferencia.MES_ANTERIOR, null,
                    new BigDecimal("200"), SETEMBRO);
            when(metaEconomiaRepository.listarParaCongelar(USUARIO_ID, OUTUBRO)).thenReturn(List.of(meta));
            when(transacaoRepository.buscarPrimeiraDataOcorrencia(USUARIO_ID)).thenReturn(LocalDate.of(2026, 10, 2));

            //Act
            metaEconomiaService.listar(USUARIO_ID, OUTUBRO);

            //Assert
            assertTrue(meta.isCancelada());
        }
    }

    @Nested
    class cancelarPendentesDaCategoria {

        @Test
        @DisplayName("Deve cancelar apenas as metas pendentes da categoria")
        void deveCancelarApenasPendentes() {

            //Arrange
            var pendente = new MetaEconomia(USUARIO_ID, mercado, NOVEMBRO, TipoReferencia.MES_ANTERIOR, null,
                    BigDecimal.TEN, OUTUBRO);
            var naoAtingida = new MetaEconomia(USUARIO_ID, mercado, OUTUBRO, TipoReferencia.MES_ANTERIOR, null,
                    new BigDecimal("200"), OUTUBRO);
            naoAtingida.congelar(new BigDecimal("1000"), Instant.now(CLOCK));
            when(metaEconomiaRepository.findByUsuarioIdAndCategoriaIdAndCanceladaEmIsNull(USUARIO_ID, CATEGORIA_ID))
                    .thenReturn(List.of(pendente, naoAtingida));
            prepararGasto(NOVEMBRO, NOVEMBRO, "0");
            prepararGasto(OUTUBRO, OUTUBRO, "900");

            //Act
            metaEconomiaService.cancelarPendentesDaCategoria(USUARIO_ID, CATEGORIA_ID);

            //Assert
            assertTrue(pendente.isCancelada());
            assertFalse(naoAtingida.isCancelada());
            verify(metaEconomiaRepository).listarParaCongelar(eq(USUARIO_ID), eq(OUTUBRO));
        }
    }
}
