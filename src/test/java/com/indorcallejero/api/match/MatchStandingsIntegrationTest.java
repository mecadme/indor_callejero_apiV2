package com.indorcallejero.api.match;

import com.indorcallejero.api.AbstractIntegrationTest;
import com.indorcallejero.api.standing.StandingRepository;
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

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PERF-03 hecho ejecutable de punta a punta: registra un resultado por
 * HTTP, confirma que la respuesta vuelve YA (sin esperar standings), y
 * recién después usa Awaitility para confirmar que el listener async
 * terminó actualizando ambos equipos -- sin Awaitility, este test tendría
 * que adivinar cuánto dormir, la falla clásica de testear código async.
 */
@AutoConfigureMockMvc
class MatchStandingsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StandingRepository standingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void registrarResultado_respondeInmediato_yActualizaStandingsAsync() throws Exception {
        String adminToken = registerAdminAndLogin("admin-match-it");
        Long homeTeamId = createTeam(adminToken, "Los Tigres");
        Long awayTeamId = createTeam(adminToken, "Los Leones");
        Long matchId = createMatch(adminToken, homeTeamId, awayTeamId);

        mockMvc.perform(post("/api/matches/" + matchId + "/start")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/matches/" + matchId + "/result")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"goalsHomeTeam":2,"goalsAwayTeam":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var homeStanding = standingRepository.findByTeamId(homeTeamId).orElseThrow();
            assertThat(homeStanding.getWins()).isEqualTo(1);
            assertThat(homeStanding.getPoints()).isEqualTo(3);
            assertThat(homeStanding.getGoalsFor()).isEqualTo(2);

            var awayStanding = standingRepository.findByTeamId(awayTeamId).orElseThrow();
            assertThat(awayStanding.getLosses()).isEqualTo(1);
            assertThat(awayStanding.getPoints()).isZero();
        });
    }

    @Test
    void cargarResultado_sobreUnPartidoNoIniciado_esRechazadoConConflict() throws Exception {
        String adminToken = registerAdminAndLogin("admin-conflict-it");
        Long homeTeamId = createTeam(adminToken, "Equipo A");
        Long awayTeamId = createTeam(adminToken, "Equipo B");
        Long matchId = createMatch(adminToken, homeTeamId, awayTeamId);

        mockMvc.perform(post("/api/matches/" + matchId + "/result")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"goalsHomeTeam":1,"goalsAwayTeam":0}
                                """))
                .andExpect(status().isConflict());
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

    private Long createMatch(String adminToken, Long homeTeamId, Long awayTeamId) throws Exception {
        String scheduledAt = Instant.now().plus(1, ChronoUnit.DAYS).toString();
        MvcResult result = mockMvc.perform(post("/api/matches")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"homeTeamId":%d,"awayTeamId":%d,"scheduledAt":"%s"}
                                """.formatted(homeTeamId, awayTeamId, scheduledAt)))
                .andExpect(status().isCreated())
                .andReturn();
        ObjectNode body = (ObjectNode) objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }
}
