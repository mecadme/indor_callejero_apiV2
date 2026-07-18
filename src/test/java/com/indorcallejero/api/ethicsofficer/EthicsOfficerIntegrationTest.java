package com.indorcallejero.api.ethicsofficer;

import com.indorcallejero.api.AbstractIntegrationTest;
import com.indorcallejero.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class EthicsOfficerIntegrationTest extends AbstractIntegrationTest {

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void crearOficial_subirleFoto_yAsignarloAUnEquipo() throws Exception {
        String adminToken = registerAdminAndLogin("admin-ethics-it");
        Long officerId = createEthicsOfficer(adminToken, "Ana", "Gomez");
        Long teamId = createTeam(adminToken, "Ethics Team");

        MockMultipartFile photo = new MockMultipartFile("file", "foto.png", "image/png", PNG_SIGNATURE);
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/ethics-officers/" + officerId + "/photo")
                        .file(photo)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").value(org.hamcrest.Matchers.startsWith("/api/files/ethics-officers/")));

        mockMvc.perform(patch("/api/ethics-officers/" + officerId + "/team")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"teamId":%d}
                                """.formatted(teamId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamId").value(teamId))
                .andExpect(jsonPath("$.teamName").value("Ethics Team"));
    }

    @Test
    void usuarioSinRolAdmin_noPuedeCrearOficialesDeEtica() throws Exception {
        String userToken = registerPlainUserAndLogin("plain-user-ethics-it");

        mockMvc.perform(post("/api/ethics-officers")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ana","lastName":"Gomez"}
                                """))
                .andExpect(status().isForbidden());
    }

    private String registerAdminAndLogin(String username) throws Exception {
        registerPlainUser(username);
        Long userId = userRepository.findByUsername(username).orElseThrow().getId();
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role) VALUES (?, ?)", userId, "ADMIN");
        return login(username);
    }

    private String registerPlainUserAndLogin(String username) throws Exception {
        registerPlainUser(username);
        return login(username);
    }

    private void registerPlainUser(String username) throws Exception {
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

    private Long createEthicsOfficer(String adminToken, String firstName, String lastName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/ethics-officers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"%s","lastName":"%s"}
                                """.formatted(firstName, lastName)))
                .andExpect(status().isCreated())
                .andReturn();
        ObjectNode body = (ObjectNode) objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
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
}
