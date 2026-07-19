package com.indorcallejero.api.playerstatistics;

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
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba el enfoque "mezclable" charlado antes de escribir código: un
 * mismo endpoint (POST .../events) recibe un LOTE de eventos acá -- el
 * mismo camino serviría para 1 evento por vez si algún día existe un
 * reloj de partido en vivo (Etapa 11, gap conocido).
 */
@AutoConfigureMockMvc
class PlayerStatisticsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void cargarEventosEnLote_yConsultarLasAgregacionesPorJugadorYPorPartido() throws Exception {
        String adminToken = registerAdminAndLogin("admin-stats-it");
        Long homeTeamId = createTeam(adminToken, "Stats Home");
        Long awayTeamId = createTeam(adminToken, "Stats Away");
        Long scorerId = createPlayer(adminToken, "Leo", "Diaz", 9);
        Long assisterId = createPlayer(adminToken, "Juan", "Perez", 7);
        Long matchId = createMatch(adminToken, homeTeamId, awayTeamId);

        mockMvc.perform(post("/api/matches/" + matchId + "/start")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/player-statistics/matches/" + matchId + "/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"events":[
                                    {"playerId":%d,"statType":"GOAL","minute":34},
                                    {"playerId":%d,"statType":"ASSIST","minute":34},
                                    {"playerId":%d,"statType":"GOAL","minute":70}
                                ]}
                                """.formatted(scorerId, assisterId, scorerId)))
                .andExpect(status().isCreated());

        MvcResult matchEventsResult = mockMvc.perform(get("/api/player-statistics/matches/" + matchId + "/events")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        ObjectNode matchEventsBody = (ObjectNode) objectMapper.readTree(matchEventsResult.getResponse().getContentAsString());
        assertThat(matchEventsBody.get("page").get("totalElements").asInt()).isEqualTo(3);

        mockMvc.perform(get("/api/player-statistics/matches/" + matchId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.playerId == " + scorerId + " && @.statType == 'GOAL')].count").value(2))
                .andExpect(jsonPath("$[?(@.playerId == " + assisterId + " && @.statType == 'ASSIST')].count").value(1));

        MvcResult playerStatsResult = mockMvc.perform(get("/api/player-statistics/players/" + scorerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        ArrayNode playerStats = (ArrayNode) objectMapper.readTree(playerStatsResult.getResponse().getContentAsString());
        assertThat(playerStats).hasSize(1);
        assertThat(playerStats.get(0).get("count").asInt()).isEqualTo(2);
    }

    @Test
    void cargarEventos_esRechazado_siElPartidoNoEstaEnCurso() throws Exception {
        String adminToken = registerAdminAndLogin("admin-stats-conflict-it");
        Long homeTeamId = createTeam(adminToken, "Conflict Home");
        Long awayTeamId = createTeam(adminToken, "Conflict Away");
        Long playerId = createPlayer(adminToken, "Leo", "Diaz", 9);
        Long matchId = createMatch(adminToken, homeTeamId, awayTeamId);

        mockMvc.perform(post("/api/player-statistics/matches/" + matchId + "/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"events":[{"playerId":%d,"statType":"GOAL","minute":10}]}
                                """.formatted(playerId)))
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

    private Long createPlayer(String adminToken, String firstName, String lastName, int jerseyNumber) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/players")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"%s","lastName":"%s","jerseyNumber":%d,"position":"ATTACKER","age":25,"height":1.75}
                                """.formatted(firstName, lastName, jerseyNumber)))
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
