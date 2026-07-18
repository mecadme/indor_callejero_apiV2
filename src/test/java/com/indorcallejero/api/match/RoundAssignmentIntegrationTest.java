package com.indorcallejero.api.match;

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

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Etapa 12 (Round): la relación MatchEntity.round es LAZY, igual que
 * homeTeam/awayTeam -- esto prueba que se resuelve de verdad a través de
 * Hibernate real (no un mock) tanto al asignar como al desasignar.
 */
@AutoConfigureMockMvc
class RoundAssignmentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void asignarYDesasignarLaRondaDeUnPartido_quedaReflejadoEnElCalendario() throws Exception {
        String adminToken = registerAdminAndLogin("admin-round-it");
        Long homeTeamId = createTeam(adminToken, "Round Home");
        Long awayTeamId = createTeam(adminToken, "Round Away");
        Long roundId = createRound(adminToken, "Fecha 1", 1);
        Long matchId = createMatch(adminToken, homeTeamId, awayTeamId, null);

        mockMvc.perform(get("/api/matches/" + matchId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roundId").doesNotExist());

        mockMvc.perform(patch("/api/matches/" + matchId + "/round")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundId":%d}
                                """.formatted(roundId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roundId").value(roundId))
                .andExpect(jsonPath("$.roundName").value("Fecha 1"));

        mockMvc.perform(patch("/api/matches/" + matchId + "/round")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundId":null}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roundId").doesNotExist());
    }

    @Test
    void crearPartidoConRoundIdDeUnaRondaInexistente_esRechazado() throws Exception {
        String adminToken = registerAdminAndLogin("admin-round-404-it");
        Long homeTeamId = createTeam(adminToken, "Round 404 Home");
        Long awayTeamId = createTeam(adminToken, "Round 404 Away");

        mockMvc.perform(post("/api/matches")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"homeTeamId":%d,"awayTeamId":%d,"scheduledAt":"%s","roundId":999999}
                                """.formatted(homeTeamId, awayTeamId,
                                Instant.now().plus(1, ChronoUnit.DAYS))))
                .andExpect(status().isNotFound());
    }

    private String registerAdminAndLogin(String username) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Test","lastName":"User","username":"%s","password":"claveSegura123"}
                                """.formatted(username)))
                .andExpect(status().isOk());
        Long userId = userRepository.findByUsername(username).orElseThrow().getId();
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role) VALUES (?, ?)", userId, "ADMIN");

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

    private Long createTeam(String adminToken, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/teams")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","color":"Naranja","neighborhood":"Centro","group":"A1"}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        ObjectNode body = (ObjectNode) objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    private Long createRound(String adminToken, String name, int number) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/rounds")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","number":%d}
                                """.formatted(name, number)))
                .andExpect(status().isCreated())
                .andReturn();
        ObjectNode body = (ObjectNode) objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    private Long createMatch(String adminToken, Long homeTeamId, Long awayTeamId, Long roundId) throws Exception {
        String scheduledAt = Instant.now().plus(1, ChronoUnit.DAYS).toString();
        String roundIdJson = roundId == null ? "null" : roundId.toString();
        MvcResult result = mockMvc.perform(post("/api/matches")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"homeTeamId":%d,"awayTeamId":%d,"scheduledAt":"%s","roundId":%s}
                                """.formatted(homeTeamId, awayTeamId, scheduledAt, roundIdJson)))
                .andExpect(status().isCreated())
                .andReturn();
        ObjectNode body = (ObjectNode) objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }
}
