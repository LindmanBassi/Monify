package br.com.bassi.monify.domain;

import br.com.bassi.monify.TestcontainersConfiguration;
import br.com.bassi.monify.domain.enums.TipoReferencia;
import br.com.bassi.monify.domain.enums.TipoTransacao;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class MapeamentoEntidadesTest {

    private static final YearMonth OUTUBRO = YearMonth.of(2026, 10);

    @Autowired
    private TestEntityManager em;

    private Usuario usuario;
    private Categoria mercado;

    @BeforeEach
    void setUp() {
        usuario = em.persist(new Usuario("Henrique", "Henrique@Email.com ", "hash"));
        mercado = em.persist(new Categoria(usuario.getId(), "Mercado", TipoTransacao.DESPESA, "#FF0000"));
    }

    private void sincronizar() {
        em.flush();
        em.clear();
    }

    @Nested
    class entidadeBase {

        @Test
        @DisplayName("Deve gerar ID UUID versão 7 ao persistir")
        void deveGerarUuidV7AoPersistir() {

            //Assert
            assertNotNull(usuario.getId());
            assertEquals(7, usuario.getId().version());
            assertEquals(7, mercado.getId().version());
        }

        @Test
        @DisplayName("Deve preencher os timestamps de auditoria e normalizar o e-mail")
        void devePreencherTimestampsENormalizarEmail() {

            //Act
            sincronizar();
            var salvo = em.find(Usuario.class, usuario.getId());

            //Assert
            assertNotNull(salvo.getCriadoEm());
            assertNotNull(salvo.getAtualizadoEm());
            assertEquals("henrique@email.com", salvo.getEmail());
        }
    }

    @Nested
    class transacao {

        @Test
        @DisplayName("Deve persistir e recarregar a transação com a categoria")
        void devePersistirERecarregarComCategoria() {

            //Arrange
            var transacao = em.persist(new Transacao(usuario.getId(), mercado,
                    new BigDecimal("52.90"), "Feira", LocalDate.of(2026, 10, 9), LocalDate.of(2026, 10, 9)));

            //Act
            sincronizar();
            var salva = em.find(Transacao.class, transacao.getId());

            //Assert
            assertEquals(new BigDecimal("52.90"), salva.getValor());
            assertEquals(LocalDate.of(2026, 10, 9), salva.getDataOcorrencia());
            assertEquals(mercado.getId(), salva.getCategoria().getId());
            assertEquals("Mercado", salva.getCategoria().getNome());
        }
    }

    @Nested
    class orcamento {

        @Test
        @DisplayName("Deve gravar e ler o mês de referência como YearMonth")
        void deveConverterMesReferencia() {

            //Arrange
            var orcamento = em.persist(new Orcamento(usuario.getId(), mercado, OUTUBRO, new BigDecimal("1000")));

            //Act
            sincronizar();
            var salvo = em.find(Orcamento.class, orcamento.getId());

            //Assert
            assertEquals(OUTUBRO, salvo.getMesReferencia());
        }

        @Test
        @DisplayName("Deve lançar exceção quando houver dois orçamentos para a mesma categoria e mês")
        void deveLancarExcecaoQuandoOrcamentoDuplicado() {

            //Arrange
            em.persist(new Orcamento(usuario.getId(), mercado, OUTUBRO, new BigDecimal("1000")));
            em.persist(new Orcamento(usuario.getId(), mercado, OUTUBRO, new BigDecimal("2000")));

            //Act & Assert
            assertThrows(PersistenceException.class, () -> em.flush());
        }
    }

    @Nested
    class metaEconomia {

        private MetaEconomia novaMeta() {
            return new MetaEconomia(usuario.getId(), mercado, OUTUBRO, TipoReferencia.MES_ANTERIOR,
                    null, new BigDecimal("100"), OUTUBRO);
        }

        @Test
        @DisplayName("Deve lançar exceção quando houver duas metas ativas para a mesma categoria e mês")
        void deveLancarExcecaoQuandoDuasMetasAtivas() {

            //Arrange
            em.persist(novaMeta());
            em.persist(novaMeta());

            //Act & Assert
            assertThrows(PersistenceException.class, () -> em.flush());
        }

        @Test
        @DisplayName("Deve permitir nova meta quando as anteriores estiverem canceladas")
        void devePermitirNovaMetaQuandoAnterioresCanceladas() {

            //Arrange
            var cancelada1 = novaMeta();
            cancelada1.cancelar(BigDecimal.ZERO, OUTUBRO, Instant.now());
            var cancelada2 = novaMeta();
            cancelada2.cancelar(BigDecimal.ZERO, OUTUBRO, Instant.now());

            //Act
            em.persist(cancelada1);
            em.persist(cancelada2);
            var ativa = em.persist(novaMeta());

            //Assert
            assertDoesNotThrow(() -> em.flush());
            assertNotNull(ativa.getId());
        }

        @Test
        @DisplayName("Deve persistir os valores congelados")
        void devePersistirValoresCongelados() {

            //Arrange
            var meta = novaMeta();
            meta.congelar(new BigDecimal("1000"), Instant.now());
            em.persist(meta);

            //Act
            sincronizar();
            var salva = em.find(MetaEconomia.class, meta.getId());

            //Assert
            assertTrue(salva.isCongelada());
            assertEquals(new BigDecimal("1000.00"), salva.getValorReferencia());
            assertEquals(new BigDecimal("900.00"), salva.getValorLimite());
        }
    }

    @Nested
    class metaPoupanca {

        @Test
        @DisplayName("Deve persistir os aportes em cascata junto com a meta")
        void devePersistirAportesEmCascata() {

            //Arrange
            var viagem = new MetaPoupanca(usuario.getId(), "Viagem", new BigDecimal("1000"), null);
            viagem.registrarAporte(new BigDecimal("300"), LocalDate.of(2026, 10, 1), "Salário");
            viagem.registrarAporte(new BigDecimal("-50"), LocalDate.of(2026, 10, 5), null);
            em.persist(viagem);

            //Act
            sincronizar();
            var salva = em.find(MetaPoupanca.class, viagem.getId());

            //Assert
            assertEquals(2, salva.getAportes().size());
            assertEquals(new BigDecimal("250.00"), salva.calcularAcumulado());
        }
    }
}
