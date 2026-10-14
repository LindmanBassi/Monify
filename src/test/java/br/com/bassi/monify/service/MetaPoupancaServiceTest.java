package br.com.bassi.monify.service;

import br.com.bassi.monify.domain.MetaPoupanca;
import br.com.bassi.monify.dto.CriarMetaPoupancaRequest;
import br.com.bassi.monify.dto.RegistrarAporteRequest;
import br.com.bassi.monify.exception.RecursoNaoEncontradoException;
import br.com.bassi.monify.repository.AcumuladoPorMeta;
import br.com.bassi.monify.repository.AporteRepository;
import br.com.bassi.monify.repository.MetaPoupancaRepository;
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
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetaPoupancaServiceTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID META_ID = UUID.randomUUID();
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 15);

    @Mock
    private MetaPoupancaRepository metaPoupancaRepository;

    @Mock
    private AporteRepository aporteRepository;

    private MetaPoupancaService metaPoupancaService;

    @BeforeEach
    void setUp() {
        metaPoupancaService = new MetaPoupancaService(metaPoupancaRepository, aporteRepository, CLOCK);
    }

    private MetaPoupanca viagemComId() {
        var meta = new MetaPoupanca(USUARIO_ID, "Viagem", new BigDecimal("1200"), LocalDate.of(2026, 12, 31), HOJE);
        ReflectionTestUtils.setField(meta, "id", META_ID);
        return meta;
    }

    @Nested
    class listar {

        @Test
        @DisplayName("Deve usar o acumulado somado no banco para cada meta")
        void deveUsarAcumuladoSomadoNoBanco() {

            //Arrange
            var acumulado = mock(AcumuladoPorMeta.class);
            when(acumulado.getMetaPoupancaId()).thenReturn(META_ID);
            when(acumulado.getTotal()).thenReturn(new BigDecimal("300.00"));
            when(aporteRepository.somarPorMeta(USUARIO_ID)).thenReturn(List.of(acumulado));
            when(metaPoupancaRepository.listar(USUARIO_ID, null)).thenReturn(List.of(viagemComId()));

            //Act
            var meta = metaPoupancaService.listar(USUARIO_ID, null).getFirst();

            //Assert
            assertEquals(new BigDecimal("300.00"), meta.acumulado());
            assertEquals(new BigDecimal("25.00"), meta.percentual());
            assertEquals(new BigDecimal("900.00"), meta.faltante());
            assertEquals(new BigDecimal("300.00"), meta.valorPorMes());
        }
    }

    @Nested
    class criar {

        @Test
        @DisplayName("Deve criar a meta ativa para o usuário com acumulado zero")
        void deveCriarMetaAtiva() {

            //Arrange
            when(metaPoupancaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            //Act
            var meta = metaPoupancaService.criar(USUARIO_ID, new CriarMetaPoupancaRequest("Viagem", new BigDecimal("1000"), null));

            //Assert
            assertEquals(new BigDecimal("0.00"), meta.acumulado());
            assertNull(meta.valorPorMes());
            assertFalse(meta.prazoVencido());
        }
    }

    @Nested
    class registrarAporte {

        @Test
        @DisplayName("Deve registrar o aporte na meta do usuário e salvá-lo")
        void deveRegistrarAporte() {

            //Arrange
            var meta = viagemComId();
            when(metaPoupancaRepository.findByIdAndUsuarioId(META_ID, USUARIO_ID)).thenReturn(Optional.of(meta));
            when(aporteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            //Act
            var aporte = metaPoupancaService.registrarAporte(USUARIO_ID, META_ID,
                    new RegistrarAporteRequest(new BigDecimal("250"), HOJE, "Salário"));

            //Assert
            assertEquals(new BigDecimal("250.00"), aporte.valor());
            assertEquals(new BigDecimal("250.00"), meta.calcularAcumulado());
        }

        @Test
        @DisplayName("Deve lançar não encontrado quando a meta não for do usuário")
        void deveLancarNaoEncontradoQuandoMetaDeOutroUsuario() {

            //Arrange
            when(metaPoupancaRepository.findByIdAndUsuarioId(META_ID, USUARIO_ID)).thenReturn(Optional.empty());

            //Act & Assert
            var excecao = assertThrows(RecursoNaoEncontradoException.class, () -> metaPoupancaService.registrarAporte(
                    USUARIO_ID, META_ID, new RegistrarAporteRequest(BigDecimal.TEN, HOJE, null)));
            assertEquals("Meta de poupança não encontrada", excecao.getMessage());
            verifyNoInteractions(aporteRepository);
        }
    }
}
