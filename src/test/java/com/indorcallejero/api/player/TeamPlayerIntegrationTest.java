package com.indorcallejero.api.player;

import com.indorcallejero.api.AbstractIntegrationTest;
import com.indorcallejero.api.user.UserRepository;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import jakarta.persistence.EntityManagerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PERF-02 del audit, hecho ejecutable: no alcanza con mirar el log SQL una
 * vez en la Etapa 6 y confiar en que siga así. Este test cuenta queries de
 * verdad vía Hibernate Statistics -- si alguien saca el @EntityGraph de
 * PlayerRepository el día de mañana, este test lo rompe en el momento, no
 * un usuario notando que el listado de jugadores se puso lento.
 */
@AutoConfigureMockMvc
class TeamPlayerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void listarJugadores_conEquipo_noDisparaNMasUnoQueries() throws Exception {
        String adminToken = registerAdminAndLogin("admin-perf-it");
        Long teamId = createTeam(adminToken, "Los Tigres", "A1");

        for (int i = 1; i <= 5; i++) {
            Long playerId = createPlayer(adminToken, "Jugador" + i, i);
            assignTeam(adminToken, playerId, teamId);
        }

        Statistics statistics = statistics();
        statistics.clear();

        MvcResult result = mockMvc.perform(get("/api/players?size=10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).containsSubsequence("\"teamName\"", "\"Los Tigres\"");

        // Página de 5 jugadores con equipo: 1 query de conteo (paginación) +
        // 1 select con LEFT JOIN a team. Si esto alguna vez sube a 6+
        // (una query por jugador), el @EntityGraph se rompió.
        assertThat(statistics.getPrepareStatementCount())
                .as("cantidad de queries SQL preparadas para listar 5 jugadores con equipo")
                .isLessThanOrEqualTo(2);
    }

    @Test
    void crearEquipo_sinRolAdmin_esRechazadoConForbidden() throws Exception {
        String userToken = registerAndLogin("usuario-normal-teams-it");

        mockMvc.perform(post("/api/teams")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Equipo Trucho","group":"A2"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTeam_inexistente_devuelve404Limpio() throws Exception {
        String adminToken = registerAdminAndLogin("admin-404-it");

        mockMvc.perform(get("/api/teams/999999").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void jugadorSinEquipo_apareceEnWithoutTeam() throws Exception {
        String adminToken = registerAdminAndLogin("admin-noteam-it");
        createPlayer(adminToken, "Sin Equipo", 77);

        mockMvc.perform(get("/api/players/without-team")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.firstName=='Sin Equipo')]").exists());
    }

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    // Insert por JDBC crudo a propósito, no user.grantRole(...) + save(): esto
    // último tocaría la colección LAZY "roles" fuera de una transacción
    // abierta y volvería a explotar con el mismo LazyInitializationException
    // que ya resolvimos en AuthService (Etapa 2) -- acá directamente evitamos
    // la colección gestionada por Hibernate en vez de andar abriendo una
    // transacción solo para este bootstrap de test.
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

    private Long createTeam(String adminToken, String name, String group) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/teams")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","color":"Naranja","neighborhood":"Centro","group":"%s"}
                                """.formatted(name, group)))
                .andExpect(status().isCreated())
                .andReturn();
        ObjectNode body = (ObjectNode) objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    private Long createPlayer(String adminToken, String firstName, int jerseyNumber) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/players")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"%s","lastName":"Apellido","jerseyNumber":%d,"position":"MIDFIELDER","age":25,"height":1.75}
                                """.formatted(firstName, jerseyNumber)))
                .andExpect(status().isCreated())
                .andReturn();
        ObjectNode body = (ObjectNode) objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    private void assignTeam(String adminToken, Long playerId, Long teamId) throws Exception {
        mockMvc.perform(patch("/api/players/" + playerId + "/team")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"teamId":%d}
                                """.formatted(teamId)))
                .andExpect(status().isOk());
    }
}
