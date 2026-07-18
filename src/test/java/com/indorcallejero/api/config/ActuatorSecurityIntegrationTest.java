package com.indorcallejero.api.config;

import com.indorcallejero.api.AbstractIntegrationTest;
import com.indorcallejero.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PERF-09: /health tiene que quedar accesible para un load balancer sin
 * JWT, pero el resto de Actuator (métricas, uso de memoria, conteo de
 * requests por endpoint) es información interna -- ni siquiera alcanza
 * con estar autenticado, hace falta ser ADMIN.
 */
@AutoConfigureMockMvc
class ActuatorSecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void health_esPublico_sinToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void metrics_rechazaSinToken() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void metrics_rechazaUsuarioAutenticado_sinRolAdmin() throws Exception {
        String userToken = registerAndLogin("usuario-metrics-it");

        mockMvc.perform(get("/actuator/metrics").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void metrics_permiteAdmin() throws Exception {
        String adminToken = registerAdminAndLogin("admin-metrics-it");

        mockMvc.perform(get("/actuator/metrics").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private String registerAdminAndLogin(String username) throws Exception {
        register(username);
        Long userId = userRepository.findByUsername(username).orElseThrow().getId();
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role) VALUES (?, ?)", userId, "ADMIN");
        return login(username);
    }

    private String registerAndLogin(String username) throws Exception {
        register(username);
        return login(username);
    }

    private void register(String username) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Test","lastName":"User","username":"%s","password":"claveSegura123"}
                                """.formatted(username)))
                .andExpect(status().isOk());
    }

    private String login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"claveSegura123"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn();
        ObjectNode body = (ObjectNode) objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }
}
