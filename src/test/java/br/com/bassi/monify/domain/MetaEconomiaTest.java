package br.com.bassi.monify.domain;

import br.com.bassi.monify.domain.enums.StatusMetaEconomia;
import br.com.bassi.monify.domain.enums.TipoReferencia;
import br.com.bassi.monify.domain.enums.TipoTransacao;
import br.com.bassi.monify.exception.RegraNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MetaEconomiaTest {

    private static final YearMonth SETEMBRO = YearMonth.of(2026, 9);
    private static final YearMonth OUTUBRO = YearMonth.of(2026, 10);
    private static final YearMonth NOVEMBRO = YearMonth.of(2026, 11);
    private static final Instant AGORA = Instant.parse("2026-10-09T12:00:00Z");

    private final UUID usuarioId = UUID.randomUUID();
    private final Categoria mercado = new Categoria(usuarioId, "Mercado", TipoTransacao.DESPESA, "#FF0000");

    private MetaEconomia metaMesAnterior(YearMonth mes, String economia) {
        return new MetaEconomia(usuarioId, mercado, mes, TipoReferencia.MES_ANTERIOR, null,
                new BigDecimal(economia), OUTUBRO);
    }

    private MetaEconomia metaCongelada() {
        var meta = metaMesAnterior(OUTUBRO, "200");
        meta.congelar(new BigDecimal("1000"), AGORA);
        return meta;
    }

    @Nested
    class criar {

        @Test
        @DisplayName("Deve criar meta para o mês atual ou futuro")
        void deveCriarParaMesAtualOuFuturo() {

            //Act
            var metaOutubro = metaMesAnterior(OUTUBRO, "100");
            var metaNovembro = metaMesAnterior(NOVEMBRO, "100");

            //Assert
            assertEquals(OUTUBRO, metaOutubro.getMesReferencia());
            assertEquals(NOVEMBRO, metaNovembro.getMesReferencia());
        }

        @Test
        @DisplayName("Deve lançar exceção quando o mês for passado")
        void deveLancarExcecaoQuandoMesPassado() {

            //Act & Assert
            assertThrows(RegraNegocioException.class, () -> metaMesAnterior(SETEMBRO, "100"));
        }

        @Test
        @DisplayName("Deve lançar exceção quando a categoria for de receita")
        void deveLancarExcecaoQuandoCategoriaDeReceita() {

            //Arrange
            var salario = new Categoria(usuarioId, "Salário", TipoTransacao.RECEITA, "#00FF00");

            //Act & Assert
            assertThrows(RegraNegocioException.class, () -> new MetaEconomia(usuarioId, salario, OUTUBRO,
                    TipoReferencia.MES_ANTERIOR, null, BigDecimal.TEN, OUTUBRO));
        }

        @Test
        @DisplayName("Deve lançar exceção quando a categoria estiver arquivada")
        void deveLancarExcecaoQuandoCategoriaArquivada() {

            //Arrange
            mercado.arquivar();

            //Act & Assert
            assertThrows(RegraNegocioException.class, () -> metaMesAnterior(OUTUBRO, "100"));
        }

        @Test
        @DisplayName("Deve lançar exceção quando o valor de economia não for positivo")
        void deveLancarExcecaoQuandoEconomiaNaoPositiva() {

            //Act & Assert
            assertThrows(RegraNegocioException.class, () -> metaMesAnterior(OUTUBRO, "0"));
        }

        @Test
        @DisplayName("Deve lançar exceção quando a referência for ORCAMENTO e não houver orçamento")
        void deveLancarExcecaoQuandoSemOrcamento() {

            //Act & Assert
            assertThrows(RegraNegocioException.class, () -> new MetaEconomia(usuarioId, mercado, OUTUBRO,
                    TipoReferencia.ORCAMENTO, null, BigDecimal.TEN, OUTUBRO));
        }

        @Test
        @DisplayName("Deve lançar exceção quando o orçamento for de outro mês")
        void deveLancarExcecaoQuandoOrcamentoDeOutroMes() {

            //Arrange
            var orcamentoNovembro = new Orcamento(usuarioId, mercado, NOVEMBRO, new BigDecimal("1000"));

            //Act & Assert
            assertThrows(RegraNegocioException.class, () -> new MetaEconomia(usuarioId, mercado, OUTUBRO,
                    TipoReferencia.ORCAMENTO, orcamentoNovembro, BigDecimal.TEN, OUTUBRO));
        }

        @Test
        @DisplayName("Deve vincular o orçamento quando a referência for ORCAMENTO")
        void deveVincularOrcamentoQuandoReferenciaOrcamento() {

            //Arrange
            var orcamentoOutubro = new Orcamento(usuarioId, mercado, OUTUBRO, new BigDecimal("1000"));

            //Act
            var meta = new MetaEconomia(usuarioId, mercado, OUTUBRO,
                    TipoReferencia.ORCAMENTO, orcamentoOutubro, BigDecimal.TEN, OUTUBRO);

            //Assert
            assertSame(orcamentoOutubro, meta.getOrcamento());
        }

        @Test
        @DisplayName("Deve lançar exceção quando informar orçamento para outra referência")
        void deveLancarExcecaoQuandoOrcamentoParaOutraReferencia() {

            //Arrange
            var orcamento = new Orcamento(usuarioId, mercado, OUTUBRO, new BigDecimal("1000"));

            //Act & Assert
            assertThrows(RegraNegocioException.class, () -> new MetaEconomia(usuarioId, mercado, OUTUBRO,
                    TipoReferencia.MES_ANTERIOR, orcamento, BigDecimal.TEN, OUTUBRO));
        }
    }

    @Nested
    class congelar {

        @Test
        @DisplayName("Deve indicar congelamento somente quando o mês da meta começar")
        void deveCongelarQuandoMesComecar() {

            //Arrange
            var meta = metaMesAnterior(NOVEMBRO, "100");

            //Act & Assert
            assertFalse(meta.deveCongelar(OUTUBRO));
            assertTrue(meta.deveCongelar(NOVEMBRO));
        }

        @Test
        @DisplayName("Deve congelar a referência e calcular o limite")
        void deveCongelarReferenciaELimite() {

            //Act
            var meta = metaCongelada();

            //Assert
            assertTrue(meta.isCongelada());
            assertEquals(new BigDecimal("1000.00"), meta.getValorReferencia());
            assertEquals(new BigDecimal("800.00"), meta.getValorLimite());
            assertEquals(AGORA, meta.getCongeladaEm());
            assertFalse(meta.deveCongelar(OUTUBRO));
        }

        @Test
        @DisplayName("Deve lançar exceção quando a meta já estiver congelada")
        void deveLancarExcecaoQuandoJaCongelada() {

            //Arrange
            var meta = metaCongelada();

            //Act & Assert
            assertThrows(RegraNegocioException.class, () -> meta.congelar(new BigDecimal("2000"), AGORA));
        }

        @Test
        @DisplayName("Deve lançar exceção quando o limite calculado não for positivo")
        void deveLancarExcecaoQuandoLimiteNaoPositivo() {

            //Arrange
            var meta = metaMesAnterior(OUTUBRO, "500");

            //Act & Assert
            assertThrows(RegraNegocioException.class, () -> meta.congelar(new BigDecimal("500"), AGORA));
            assertFalse(meta.isCongelada());
        }
    }

    @Nested
    class editar {

        @Test
        @DisplayName("Deve editar a meta antes do congelamento")
        void deveEditarAntesDoCongelamento() {

            //Arrange
            var meta = metaMesAnterior(NOVEMBRO, "100");

            //Act
            meta.editar(TipoReferencia.MEDIA_TRES_MESES, null, new BigDecimal("150"));

            //Assert
            assertEquals(TipoReferencia.MEDIA_TRES_MESES, meta.getTipoReferencia());
            assertEquals(new BigDecimal("150.00"), meta.getValorEconomia());
        }

        @Test
        @DisplayName("Deve lançar exceção quando a meta estiver congelada")
        void deveLancarExcecaoQuandoCongelada() {

            //Arrange
            var meta = metaCongelada();

            //Act & Assert
            assertThrows(RegraNegocioException.class,
                    () -> meta.editar(TipoReferencia.MES_ANTERIOR, null, new BigDecimal("50")));
        }
    }

    @Nested
    class calcularStatus {

        @Test
        @DisplayName("Deve retornar AGUARDANDO_INICIO antes do congelamento")
        void deveRetornarAguardandoInicioAntesDoCongelamento() {

            //Act
            var status = metaMesAnterior(NOVEMBRO, "100").calcularStatus(BigDecimal.ZERO, OUTUBRO);

            //Assert
            assertEquals(StatusMetaEconomia.AGUARDANDO_INICIO, status);
        }

        @Test
        @DisplayName("Deve retornar EM_ANDAMENTO durante o mês dentro do limite")
        void deveRetornarEmAndamentoDentroDoLimite() {

            //Act
            var status = metaCongelada().calcularStatus(new BigDecimal("800"), OUTUBRO);

            //Assert
            assertEquals(StatusMetaEconomia.EM_ANDAMENTO, status);
        }

        @Test
        @DisplayName("Deve retornar NAO_ATINGIDA assim que ultrapassar o limite, mesmo antes do fim do mês")
        void deveRetornarNaoAtingidaAntesDoFimDoMes() {

            //Act
            var status = metaCongelada().calcularStatus(new BigDecimal("800.01"), OUTUBRO);

            //Assert
            assertEquals(StatusMetaEconomia.NAO_ATINGIDA, status);
        }

        @Test
        @DisplayName("Deve retornar ATINGIDA somente após o fim do mês")
        void deveRetornarAtingidaAposFimDoMes() {

            //Act
            var status = metaCongelada().calcularStatus(new BigDecimal("700"), NOVEMBRO);

            //Assert
            assertEquals(StatusMetaEconomia.ATINGIDA, status);
        }

        @Test
        @DisplayName("Deve retornar NAO_ATINGIDA após o fim do mês quando ultrapassou o limite")
        void deveRetornarNaoAtingidaAposFimDoMes() {

            //Act
            var status = metaCongelada().calcularStatus(new BigDecimal("900"), NOVEMBRO);

            //Assert
            assertEquals(StatusMetaEconomia.NAO_ATINGIDA, status);
        }

        @Test
        @DisplayName("Deve priorizar CANCELADA sobre os demais status")
        void devePriorizarCancelada() {

            //Arrange
            var meta = metaCongelada();
            meta.cancelar(BigDecimal.ZERO, OUTUBRO, AGORA);

            //Act
            var status = meta.calcularStatus(new BigDecimal("900"), NOVEMBRO);

            //Assert
            assertEquals(StatusMetaEconomia.CANCELADA, status);
        }
    }

    @Nested
    class calcularEconomiaRealizada {

        @Test
        @DisplayName("Deve calcular referência menos gasto")
        void deveCalcularReferenciaMenosGasto() {

            //Act
            var economia = metaCongelada().calcularEconomiaRealizada(new BigDecimal("750.00"));

            //Assert
            assertEquals(new BigDecimal("250.00"), economia);
        }
    }

    @Nested
    class cancelar {

        @Test
        @DisplayName("Deve cancelar metas pendentes")
        void deveCancelarMetasPendentes() {

            //Arrange
            var aguardando = metaMesAnterior(NOVEMBRO, "100");
            var emAndamento = metaCongelada();

            //Act
            aguardando.cancelar(BigDecimal.ZERO, OUTUBRO, AGORA);
            emAndamento.cancelar(new BigDecimal("100"), OUTUBRO, AGORA);

            //Assert
            assertEquals(AGORA, aguardando.getCanceladaEm());
            assertTrue(emAndamento.isCancelada());
        }

        @Test
        @DisplayName("Deve lançar exceção quando o resultado da meta já estiver definido")
        void deveLancarExcecaoQuandoResultadoDefinido() {

            //Arrange
            var naoAtingida = metaCongelada();
            var atingida = metaCongelada();

            //Act & Assert
            assertThrows(RegraNegocioException.class,
                    () -> naoAtingida.cancelar(new BigDecimal("900"), OUTUBRO, AGORA));
            assertThrows(RegraNegocioException.class,
                    () -> atingida.cancelar(new BigDecimal("100"), NOVEMBRO, AGORA));
        }

        @Test
        @DisplayName("Deve lançar exceção quando a meta já estiver cancelada")
        void deveLancarExcecaoQuandoJaCancelada() {

            //Arrange
            var meta = metaMesAnterior(NOVEMBRO, "100");
            meta.cancelar(BigDecimal.ZERO, OUTUBRO, AGORA);

            //Act & Assert
            assertThrows(RegraNegocioException.class, () -> meta.cancelar(BigDecimal.ZERO, OUTUBRO, AGORA));
        }
    }
}
