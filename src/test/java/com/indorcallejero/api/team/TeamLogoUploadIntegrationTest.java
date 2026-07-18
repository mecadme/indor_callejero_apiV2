package com.indorcallejero.api.team;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SEC-09 hecho ejecutable de punta a punta: sube un archivo real, lo
 * recupera público sin token (mismo criterio que swagger-ui en dev), y
 * confirma que un archivo con contenido falso se rechaza aunque declare
 * ser una imagen -- todo vía HTTP real, no llamando al service directo.
 */
@AutoConfigureMockMvc
class TeamLogoUploadIntegrationTest extends AbstractIntegrationTest {

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
    void subirLogo_yRecuperarloPublicoSinToken() throws Exception {
        String adminToken = registerAdminAndLogin("admin-logo-it");
        Long teamId = createTeam(adminToken, "Los Tigres");

        MockMultipartFile logo = new MockMultipartFile("file", "logo.png", "image/png", PNG_SIGNATURE);

        MvcResult uploadResult = mockMvc.perform(multipart(HttpMethod.PATCH, "/api/teams/" + teamId + "/logo")
                        .file(logo)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        ObjectNode body = (ObjectNode) objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String logoUrl = body.get("logoUrl").asText();
        assertThat(logoUrl).startsWith("/api/files/teams/").endsWith(".png");

        // Sin header de Authorization: el logo es contenido público.
        MvcResult getResult = mockMvc.perform(get(logoUrl))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(getResult.getResponse().getContentAsByteArray()).isEqualTo(PNG_SIGNATURE);
    }

    @Test
    void subirArchivoConContenidoFalso_esRechazadoAunqueDeclareSerImagen() throws Exception {
        String adminToken = registerAdminAndLogin("admin-badlogo-it");
        Long teamId = createTeam(adminToken, "Equipo Trucho");

        MockMultipartFile fake = new MockMultipartFile(
                "file", "logo.png", "image/png", "esto no es una imagen".getBytes());

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/teams/" + teamId + "/logo")
                        .file(fake)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
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
}
