package com.indorcallejero.api.information;

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

@AutoConfigureMockMvc
class InformationIntegrationTest extends AbstractIntegrationTest {

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
    void subirFoto_yRecuperarlaPublicaSinToken() throws Exception {
        String adminToken = registerAdminAndLogin("admin-info-it");
        Long infoId = createInformation(adminToken, "Nueva fecha", "El torneo arranca el sabado");

        MockMultipartFile photo = new MockMultipartFile("file", "foto.png", "image/png", PNG_SIGNATURE);

        MvcResult uploadResult = mockMvc.perform(multipart(HttpMethod.PATCH, "/api/information/" + infoId + "/photo")
                        .file(photo)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        ObjectNode body = (ObjectNode) objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String photoUrl = body.get("photoUrl").asText();
        assertThat(photoUrl).startsWith("/api/files/information/").endsWith(".png");

        MvcResult getResult = mockMvc.perform(get(photoUrl))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(getResult.getResponse().getContentAsByteArray()).isEqualTo(PNG_SIGNATURE);
    }

    @Test
    void usuarioSinRolAdmin_noPuedeCrearInformacion() throws Exception {
        String userToken = registerPlainUserAndLogin("plain-user-info-it");

        mockMvc.perform(post("/api/information")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Nueva fecha","content":"El torneo arranca el sabado"}
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

    private Long createInformation(String adminToken, String title, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/information")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","content":"%s"}
                                """.formatted(title, content)))
                .andExpect(status().isCreated())
                .andReturn();
        ObjectNode body = (ObjectNode) objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }
}
