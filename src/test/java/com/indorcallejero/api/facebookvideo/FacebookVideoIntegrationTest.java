package com.indorcallejero.api.facebookvideo;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class FacebookVideoIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void crearActualizarYBorrarUnVideo() throws Exception {
        String adminToken = registerAdminAndLogin("admin-fbvideo-it");
        Long videoId = createFacebookVideo(adminToken, "Resumen fecha 1", "https://facebook.com/video/1");

        mockMvc.perform(get("/api/facebook-videos/" + videoId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Resumen fecha 1"));

        mockMvc.perform(patch("/api/facebook-videos/" + videoId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Resumen fecha 1 (editado)","videoUrl":"https://facebook.com/video/1-editado"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Resumen fecha 1 (editado)"));

        mockMvc.perform(delete("/api/facebook-videos/" + videoId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/facebook-videos/" + videoId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void usuarioSinRolAdmin_noPuedeCrearVideos() throws Exception {
        String userToken = registerPlainUserAndLogin("plain-user-fbvideo-it");

        mockMvc.perform(post("/api/facebook-videos")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Resumen fecha 1","videoUrl":"https://facebook.com/video/1"}
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

    private Long createFacebookVideo(String adminToken, String title, String videoUrl) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/facebook-videos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","videoUrl":"%s"}
                                """.formatted(title, videoUrl)))
                .andExpect(status().isCreated())
                .andReturn();
        ObjectNode body = (ObjectNode) objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }
}
