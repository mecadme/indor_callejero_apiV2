package com.indorcallejero.api.user;

import com.indorcallejero.api.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Etapa 12: hasta acá otorgar/sacar un rol se hacía a mano con un INSERT
 * directo en user_roles (ver el helper registerAdminAndLogin de varios
 * tests de etapas anteriores) -- sin pasar por ninguna capa de
 * autorización real. Esto prueba el camino HTTP completo: que un ADMIN
 * pueda, que un USER no pueda, y que el cambio se vea reflejado de
 * verdad (Hibernate real, no un mock -- roles es LAZY en UserEntity).
 */
@AutoConfigureMockMvc
class UserRoleIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void admin_puedeOtorgarYSacarUnRol_yQuedaReflejadoEnElUsuario() throws Exception {
        String adminToken = registerAdminAndLogin("admin-roles-it");
        Long targetUserId = registerPlainUser("target-user-it");

        mockMvc.perform(post("/api/users/" + targetUserId + "/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"MANAGER"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles", org.hamcrest.Matchers.hasItem("MANAGER")));

        mockMvc.perform(get("/api/users/" + targetUserId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", org.hamcrest.Matchers.hasItem("MANAGER")));

        mockMvc.perform(delete("/api/users/" + targetUserId + "/roles/MANAGER")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("MANAGER"))));
    }

    @Test
    void usuarioSinRolAdmin_noPuedeOtorgarRoles() throws Exception {
        String userToken = registerPlainUserAndLogin("plain-user-roles-it");
        Long targetUserId = registerPlainUser("target-user-2-it");

        mockMvc.perform(post("/api/users/" + targetUserId + "/roles")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"ADMIN"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioSinRolAdmin_noPuedeSacarRoles() throws Exception {
        String userToken = registerPlainUserAndLogin("plain-user-roles-2-it");
        Long targetUserId = registerPlainUser("target-user-3-it");

        mockMvc.perform(delete("/api/users/" + targetUserId + "/roles/USER")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    private String registerAdminAndLogin(String username) throws Exception {
        Long userId = registerPlainUser(username);
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role) VALUES (?, ?)", userId, "ADMIN");
        return login(username);
    }

    private String registerPlainUserAndLogin(String username) throws Exception {
        registerPlainUser(username);
        return login(username);
    }

    private Long registerPlainUser(String username) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Test","lastName":"User","username":"%s","password":"claveSegura123"}
                                """.formatted(username)))
                .andExpect(status().isOk());
        return userRepository.findByUsername(username).orElseThrow().getId();
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
