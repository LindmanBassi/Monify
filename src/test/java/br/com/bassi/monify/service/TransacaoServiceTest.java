package br.com.bassi.monify.service;

import br.com.bassi.monify.domain.Categoria;
import br.com.bassi.monify.domain.Transacao;
import br.com.bassi.monify.domain.enums.TipoTransacao;
import br.com.bassi.monify.dto.CriarTransacaoRequest;
import br.com.bassi.monify.dto.EditarTransacaoRequest;
import br.com.bassi.monify.exception.RecursoNaoEncontradoException;
import br.com.bassi.monify.exception.RegraNegocioException;
import br.com.bassi.monify.repository.CategoriaRepository;
import br.com.bassi.monify.repository.TransacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransacaoServiceTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID CATEGORIA_ID = UUID.randomUUID();
    private static final UUID TRANSACAO_ID = UUID.randomUUID();

    // 01:30 UTC de 13/10 ainda é 12/10 em São Paulo
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-13T01:30:00Z"), ZoneId.of("America/Sao_Paulo"));
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 12);

    @Mock
    private TransacaoRepository transacaoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Captor
    private ArgumentCaptor<Transacao> transacaoCaptor;

    private TransacaoService transacaoService;

    private final Categoria mercado = categoriaComId(CATEGORIA_ID, "Mercado", TipoTransacao.DESPESA);

    @BeforeEach
    void setUp() {
        transacaoService = new TransacaoService(transacaoRepository, categoriaRepository, CLOCK);
    }

    private static Categoria categoriaComId(UUID id, String nome, TipoTransacao tipo) {
        var categoria = new Categoria(USUARIO_ID, nome, tipo, "#FF0000");
        ReflectionTestUtils.setField(categoria, "id", id);
        return categoria;
    }

    @Nested
    class criar {

        @Test
        @DisplayName("Deve salvar a transação com o tipo copiado da categoria")
        void deveSalvarComTipoDaCategoria() {

            //Arrange
            var request = new CriarTransacaoRequest(CATEGORIA_ID, new BigDecimal("52.90"), "Feira", HOJE);
            when(categoriaRepository.findByIdAndUsuarioId(CATEGORIA_ID, USUARIO_ID)).thenReturn(Optional.of(mercado));
            when(transacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            //Act
            var response = transacaoService.criar(USUARIO_ID, request);

            //Assert
            verify(transacaoRepository).save(transacaoCaptor.capture());
            var salva = transacaoCaptor.getValue();
            assertEquals(USUARIO_ID, salva.getUsuarioId());
            assertEquals(TipoTransacao.DESPESA, salva.getTipo());
            assertEquals("Mercado", response.categoria().nome());
        }

        @Test
        @DisplayName("Deve usar a data de hoje no fuso de São Paulo para bloquear datas futuras")
        void deveUsarHojeNoFusoDeSaoPaulo() {

            //Arrange
            var request = new CriarTransacaoRequest(CATEGORIA_ID, BigDecimal.TEN, null, HOJE.plusDays(1));
            when(categoriaRepository.findByIdAndUsuarioId(CATEGORIA_ID, USUARIO_ID)).thenReturn(Optional.of(mercado));

            //Act & Assert
            assertThrows(RegraNegocioException.class, () -> transacaoService.criar(USUARIO_ID, request));
            verify(transacaoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar não encontrado quando a categoria não for do usuário")
        void deveLancarNaoEncontradoQuandoCategoriaDeOutroUsuario() {

            //Arrange
            var request = new CriarTransacaoRequest(CATEGORIA_ID, BigDecimal.TEN, null, HOJE);
            when(categoriaRepository.findByIdAndUsuarioId(CATEGORIA_ID, USUARIO_ID)).thenReturn(Optional.empty());

            //Act & Assert
            var excecao = assertThrows(RecursoNaoEncontradoException.class, () -> transacaoService.criar(USUARIO_ID, request));
            assertEquals("Categoria não encontrada", excecao.getMessage());
        }
    }

    @Nested
    class editar {

        @Test
        @DisplayName("Deve atualizar a transação do usuário")
        void deveAtualizarTransacao() {

            //Arrange
            var transacao = new Transacao(USUARIO_ID, mercado, BigDecimal.TEN, null, HOJE, HOJE);
            var salarioId = UUID.randomUUID();
            var salario = categoriaComId(salarioId, "Salário", TipoTransacao.RECEITA);
            when(transacaoRepository.findByIdAndUsuarioId(TRANSACAO_ID, USUARIO_ID)).thenReturn(Optional.of(transacao));
            when(categoriaRepository.findByIdAndUsuarioId(salarioId, USUARIO_ID)).thenReturn(Optional.of(salario));

            //Act
            transacaoService.editar(USUARIO_ID, TRANSACAO_ID,
                    new EditarTransacaoRequest(salarioId, new BigDecimal("5000"), "Outubro", HOJE));

            //Assert
            assertEquals(TipoTransacao.RECEITA, transacao.getTipo());
            assertEquals(new BigDecimal("5000.00"), transacao.getValor());
        }

        @Test
        @DisplayName("Deve lançar não encontrado quando a transação não for do usuário")
        void deveLancarNaoEncontradoQuandoTransacaoDeOutroUsuario() {

            //Arrange
            when(transacaoRepository.findByIdAndUsuarioId(TRANSACAO_ID, USUARIO_ID)).thenReturn(Optional.empty());

            //Act & Assert
            var excecao = assertThrows(RecursoNaoEncontradoException.class, () -> transacaoService.editar(USUARIO_ID,
                    TRANSACAO_ID, new EditarTransacaoRequest(CATEGORIA_ID, BigDecimal.TEN, null, HOJE)));
            assertEquals("Transação não encontrada", excecao.getMessage());
            verifyNoInteractions(categoriaRepository);
        }
    }

    @Nested
    class excluir {

        @Test
        @DisplayName("Deve excluir definitivamente a transação do usuário")
        void deveExcluirTransacao() {

            //Arrange
            var transacao = new Transacao(USUARIO_ID, mercado, BigDecimal.TEN, null, HOJE, HOJE);
            when(transacaoRepository.findByIdAndUsuarioId(TRANSACAO_ID, USUARIO_ID)).thenReturn(Optional.of(transacao));

            //Act
            transacaoService.excluir(USUARIO_ID, TRANSACAO_ID);

            //Assert
            verify(transacaoRepository).delete(transacao);
        }
    }
}
