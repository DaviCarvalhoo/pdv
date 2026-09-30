/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

package br.com.balcao.pdv;

import br.com.balcao.pdv.usuario.AuthService;
import br.com.balcao.pdv.usuario.Papel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Login por PIN, perfis e autorização pontual do gerente, pela API. */
@SpringBootTest
@AutoConfigureMockMvc
class AcessoIntegracaoTest extends IntegracaoBase {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AuthService authService;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void limpar() {
        jdbc.execute(LIMPAR_BASE);
    }

    @Test
    void semTokenNaoEntra() throws Exception {
        mvc.perform(get("/api/produtos")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
        mvc.perform(get("/api/loja/publica")).andExpect(status().isOk());
    }

    @Test
    void primeiroAcessoCriaOAdministradorUmaVezSo() throws Exception {
        mvc.perform(get("/api/auth/estado")).andExpect(jsonPath("$.precisaPrimeiroAcesso").value(true));

        String token = token(mvc.perform(post("/api/auth/primeiro-acesso").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Dono\",\"pin\":\"1234\"}")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        mvc.perform(get("/api/usuarios/eu").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.papel").value("ADMIN"));
        mvc.perform(post("/api/auth/primeiro-acesso").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Outro\",\"pin\":\"9999\"}")).andExpect(status().isConflict());
    }

    @Test
    void operadorNaoVePainelENemFazSangriaSemOPinDoGerente() throws Exception {
        authService.criar("Ana", Papel.GERENTE, "2222");
        var bruno = authService.criar("Bruno", Papel.OPERADOR, "1111");
        String operador = entrar(bruno.getId(), "1111");

        mvc.perform(get("/api/relatorios/painel").header("Authorization", "Bearer " + operador))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));

        String caixa = mvc.perform(post("/api/caixas").header("Authorization", "Bearer " + operador)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"saldoInicial\":200}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long caixaId = json.readTree(caixa).get("id").asLong();

        String sangria = "{\"valor\":50,\"descricao\":\"Cofre\"}";
        mvc.perform(post("/api/caixas/" + caixaId + "/sangrias").header("Authorization", "Bearer " + operador)
                        .contentType(MediaType.APPLICATION_JSON).content(sangria))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("AUTORIZACAO_NECESSARIA"));

        mvc.perform(post("/api/auth/autorizar").contentType(MediaType.APPLICATION_JSON).content("{\"pin\":\"1111\"}"))
                .andExpect(status().isUnauthorized()); // PIN de operador não autoriza
        String autorizacao = json.readTree(mvc.perform(post("/api/auth/autorizar")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pin\":\"2222\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("autorizacao").asText();

        mvc.perform(post("/api/caixas/" + caixaId + "/sangrias").header("Authorization", "Bearer " + operador)
                        .header("X-Autorizacao", autorizacao)
                        .contentType(MediaType.APPLICATION_JSON).content(sangria))
                .andExpect(status().isCreated());
        // Autorização é de uso único.
        mvc.perform(post("/api/caixas/" + caixaId + "/sangrias").header("Authorization", "Bearer " + operador)
                        .header("X-Autorizacao", autorizacao)
                        .contentType(MediaType.APPLICATION_JSON).content(sangria))
                .andExpect(status().isForbidden());
    }

    @Test
    void pinErradoBloqueiaDepoisDeCincoTentativas() throws Exception {
        var carla = authService.criar("Carla", Papel.OPERADOR, "3333");
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/entrar").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"usuarioId\":" + carla.getId() + ",\"pin\":\"0000\"}")).andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/entrar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":" + carla.getId() + ",\"pin\":\"3333\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("bloqueado")));
    }

    private String entrar(Long usuarioId, String pin) throws Exception {
        return token(mvc.perform(post("/api/auth/entrar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":" + usuarioId + ",\"pin\":\"" + pin + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private String token(String resposta) throws Exception {
        JsonNode n = json.readTree(resposta);
        return n.get("token").asText();
    }
}
