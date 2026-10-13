package br.com.bassi.monify.controller;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

final class ApiDeTeste {

    private ApiDeTeste() {
    }

    static String cadastrarELogar(MockMvc mockMvc, String email) throws Exception {
        mockMvc.perform(post("/api/auth/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome": "Usuário de teste", "email": "%s", "senha": "senha-forte"}
                        """.formatted(email)));

        var resposta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "senha": "senha-forte"}
                                """.formatted(email)))
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(resposta, "$.tokenAcesso");
    }
}
