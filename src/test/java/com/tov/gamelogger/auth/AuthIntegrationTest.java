package com.tov.gamelogger.auth;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class AuthIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void registerThenLoginThenRefresh_fullFlowSucceeds() throws Exception {
        String email = "alice@example.com";
        String password = "correct-horse-battery";

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, password)))
                .andExpect(status().isCreated());

        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(not(blankOrNullString())))
                .andExpect(jsonPath("$.refreshToken").value(not(blankOrNullString())))
                .andReturn().getResponse().getContentAsString();

        String refreshToken = readField(loginResponse, "refreshToken");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(not(blankOrNullString())))
                .andExpect(jsonPath("$.refreshToken").value(not(blankOrNullString())));
    }

    @Test
    void register_withDuplicateEmail_returns409() throws Exception {
        String email = "duplicate@example.com";
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, "first-password")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, "second-password")))
                .andExpect(status().isConflict());
    }

    @Test
    void login_withWrongPassword_andUnknownEmail_bothReturn401WithSameMessage() throws Exception {
        String email = "known@example.com";
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, "the-real-password")))
                .andExpect(status().isCreated());

        String wrongPasswordBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String unknownEmailBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("nobody@example.com", "whatever-password")))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(readField(wrongPasswordBody, "message"))
                .isEqualTo(readField(unknownEmailBody, "message"));
    }

    @Test
    void refresh_withAccessTokenInsteadOfRefreshToken_returns401() throws Exception {
        String email = "swap@example.com";
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, "some-password")))
                .andExpect(status().isCreated());

        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, "some-password")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = readField(loginResponse, "accessToken");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(accessToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedRequest_toNonAuthPath_isRejected() throws Exception {
        mockMvc.perform(get("/library"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedRequest_withValidAccessToken_passesSecurityLayer() throws Exception {
        String email = "authed@example.com";
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, "some-password")))
                .andExpect(status().isCreated());

        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, "some-password")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = readField(loginResponse, "accessToken");

        // No /library controller exists yet (REQ-004) so this 404s once past
        // the security layer - the point of this test is that it is NOT 401.
        mockMvc.perform(get("/library").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    private String registerBody(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(new RegisterPayload(email, password));
    }

    private String loginBody(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(new RegisterPayload(email, password));
    }

    private String refreshBody(String refreshToken) throws Exception {
        return objectMapper.writeValueAsString(new RefreshPayload(refreshToken));
    }

    private String readField(String json, String field) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        return node.get(field).asText();
    }

    private record RegisterPayload(String email, String password) {
    }

    private record RefreshPayload(String refreshToken) {
    }
}
