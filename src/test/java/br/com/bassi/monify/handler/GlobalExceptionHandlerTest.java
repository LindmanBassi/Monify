package br.com.bassi.monify.handler;

import br.com.bassi.monify.exception.RegraNegocioException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ControllerDeTeste())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    class handleMonifyException {

        @Test
        @DisplayName("Deve retornar 422 com ProblemDetail quando houver violação de regra de negócio")
        void deveRetornar422QuandoRegraNegocio() throws Exception {

            //Act & Assert
            mockMvc.perform(post("/teste/regra"))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.title").value("Regra de negócio violada"))
                    .andExpect(jsonPath("$.detail").value("Mensagem da regra"));
        }
    }

    @Nested
    class handleMethodArgumentNotValidException {

        @Test
        @DisplayName("Deve retornar 400 com os campos inválidos quando a validação falhar")
        void deveRetornar400ComCamposInvalidos() throws Exception {

            //Act & Assert
            mockMvc.perform(post("/teste/validacao")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nome\": \"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Parâmetros inválidos"))
                    .andExpect(jsonPath("$['campos-invalidos'][0].campo").value("nome"))
                    .andExpect(jsonPath("$['campos-invalidos'][0].mensagem").value("O nome é obrigatório"));
        }
    }

    record RequestDeTeste(@NotBlank(message = "O nome é obrigatório") String nome) {
    }

    @RestController
    static class ControllerDeTeste {

        @PostMapping("/teste/regra")
        void regra() {
            throw new RegraNegocioException("Mensagem da regra");
        }

        @PostMapping("/teste/validacao")
        void validacao(@RequestBody @Valid RequestDeTeste request) {
        }
    }
}
