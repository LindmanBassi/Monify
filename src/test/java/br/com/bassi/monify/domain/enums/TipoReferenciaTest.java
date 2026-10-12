package br.com.bassi.monify.domain.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TipoReferenciaTest {

    private static final YearMonth OUTUBRO = YearMonth.of(2026, 10);

    @Nested
    class historicoSuficiente {

        @Test
        @DisplayName("Deve ignorar o histórico quando a referência for ORCAMENTO")
        void deveIgnorarHistoricoQuandoOrcamento() {

            //Act & Assert
            assertTrue(TipoReferencia.ORCAMENTO.historicoSuficiente(OUTUBRO, null));
        }

        @Test
        @DisplayName("Deve exigir o mês anterior dentro do histórico quando MES_ANTERIOR")
        void deveExigirMesAnteriorNoHistorico() {

            //Act & Assert
            assertTrue(TipoReferencia.MES_ANTERIOR.historicoSuficiente(OUTUBRO, YearMonth.of(2026, 9)));
            assertFalse(TipoReferencia.MES_ANTERIOR.historicoSuficiente(OUTUBRO, OUTUBRO));
            assertFalse(TipoReferencia.MES_ANTERIOR.historicoSuficiente(OUTUBRO, null));
        }

        @Test
        @DisplayName("Deve exigir três meses completos de histórico quando MEDIA_TRES_MESES")
        void deveExigirTresMesesDeHistorico() {

            //Act & Assert
            assertTrue(TipoReferencia.MEDIA_TRES_MESES.historicoSuficiente(OUTUBRO, YearMonth.of(2026, 7)));
            assertFalse(TipoReferencia.MEDIA_TRES_MESES.historicoSuficiente(OUTUBRO, YearMonth.of(2026, 8)));
        }
    }
}
