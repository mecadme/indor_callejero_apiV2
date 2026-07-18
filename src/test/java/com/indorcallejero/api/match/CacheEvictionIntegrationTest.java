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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Etapa 10 (Redis) en vivo tiró dos bugs reales que ningún test unitario
 * hubiera visto: 1) GenericJackson2JsonRedisSerializer no puede reconstruir
 * un Page cacheado (arreglado con GenericJacksonJsonRedisSerializer +
 * RestPage, ver CacheConfig y RestPage), y 2) el validator de tipos
 * polimórficos tiene que mirar la clase concreta con allowIfSubType, no el
 * tipo declarado con allowIfBaseType (arreglado en CacheConfig). Este test
 * prueba ambos caminos -- lectura repetida (cache hit real, no solo "no
 * explota") y escritura que invalida (@CacheEvict real, no datos viejos) --
 * contra un Redis real de Testcontainers, no un fake en memoria que nunca
 * hubiera reproducido ninguno de los dos bugs.
 */
@AutoConfigureMockMvc
class CacheEvictionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void getMatches_cachea_yElResultadoInvalidaElCache() throws Exception {
        String adminToken = registerAdminAndLogin("admin-cache-matches-it");
        Long homeTeamId = createTeam(adminToken, "Cache Home");
        Long awayTeamId = createTeam(adminToken, "Cache Away");
        Long matchId = createMatch(adminToken, homeTeamId, awayTeamId);

        String firstRead = getMatches(adminToken);
        assertThat(findMatchStatus(firstRead, matchId)).isEqualTo("SCHEDULED");

        // Misma Pageable, mismo request: si el segundo body es
        // byte-a-byte igual al primero (incluye a los otros tests que
        // comparten el mismo MySQL/Redis de esta suite), es porque vino
        // del cache, no de una consulta nueva a la base.
        String secondRead = getMatches(adminToken);
        assertThat(secondRead).isEqualTo(firstRead);

        mockMvc.perform(post("/api/matches/" + matchId + "/start")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // @CacheEvict(allEntries=true) es síncrono, no debería hacer falta
        // esperar -- pero corriendo la suite completa (Redis compartido,
        // más contención) se vio una falla intermitente puntual acá.
        // Awaitility en vez de asumir que un solo intento alcanza, mismo
        // criterio que ya usa MatchStandingsIntegrationTest para el
        // listener async: si nunca converge, el test igual falla.
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            String afterStart = getMatches(adminToken);
            assertThat(findMatchStatus(afterStart, matchId))
                    .as("el @CacheEvict de startMatch tiene que tirar la página vieja, no seguir devolviendo SCHEDULED")
                    .isEqualTo("IN_PROGRESS");
        });
    }

    @Test
    void getStandings_cachea_yElResultadoDelPartidoInvalidaElCache() throws Exception {
        String adminToken = registerAdminAndLogin("admin-cache-standings-it");
        Long homeTeamId = createTeam(adminToken, "Standings Home");
        Long awayTeamId = createTeam(adminToken, "Standings Away");
        Long matchId = createMatch(adminToken, homeTeamId, awayTeamId);

        String beforeResult = getStandings(adminToken);
        assertThat(findStandingGoalsFor(beforeResult, homeTeamId)).isEmpty();

        mockMvc.perform(post("/api/matches/" + matchId + "/start")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/matches/" + matchId + "/result")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"goalsHomeTeam":4,"goalsAwayTeam":0}
                                """))
                .andExpect(status().isOk());

        // El listener que puebla standings es @Async + AFTER_COMMIT (PERF-03):
        // esperamos a que termine antes de pedirle al endpoint cacheado que
        // vea el cambio, si no el test sería una carrera contra ese hilo.
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            String afterResult = getStandings(adminToken);
            assertThat(findStandingGoalsFor(afterResult, homeTeamId))
                    .as("el @CacheEvict de recordMatchResult tiene que tirar la tabla vieja, no seguir sin datos de este equipo")
                    .hasValue(4);
        });
    }

    private String getMatches(String adminToken) throws Exception {
        return mockMvc.perform(get("/api/matches").param("size", "500")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String getStandings(String adminToken) throws Exception {
        return mockMvc.perform(get("/api/standings").param("size", "500")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    // Otros tests de esta suite comparten el mismo MySQL/Redis (AbstractIntegrationTest
    // levanta un solo container por JVM), así que la lista global de matches/standings
    // trae de todo -- buscamos por id/teamId en vez de mirar el body entero.
    private String findMatchStatus(String matchesJson, Long matchId) {
        for (JsonNode node : objectMapper.readTree(matchesJson).get("content")) {
            if (node.get("id").asLong() == matchId) {
                return node.get("status").asText();
            }
        }
        throw new AssertionError("match " + matchId + " no aparece en la respuesta: " + matchesJson);
    }

    private Optional<Integer> findStandingGoalsFor(String standingsJson, Long teamId) {
        for (JsonNode node : objectMapper.readTree(standingsJson).get("content")) {
            if (node.get("teamId").asLong() == teamId) {
                return Optional.of(node.get("goalsFor").asInt());
            }
        }
        return Optional.empty();
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
