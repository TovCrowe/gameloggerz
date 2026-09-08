package com.tov.gamelogger.games;

import com.tov.gamelogger.games.dto.GameSearchResultItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class GameSearchIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    RawgClient rawgClient;

    @Test
    void search_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/games/search").param("q", "zelda"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void search_withValidTokenAndQuery_returns200WithMappedResults() throws Exception {
        String accessToken = registerAndLogin("searcher@example.com");

        when(rawgClient.search("zelda")).thenReturn(List.of(
                new GameSearchResultItem("512", "The Legend of Zelda", LocalDate.of(1986, 2, 21),
                        "https://example.com/zelda.jpg", List.of("Action", "Adventure"))));

        mockMvc.perform(get("/games/search")
                        .param("q", "zelda")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].externalId").value("512"))
                .andExpect(jsonPath("$.results[0].name").value("The Legend of Zelda"))
                .andExpect(jsonPath("$.results[0].genres[0]").value("Action"));
    }

    @Test
    void search_withMissingQueryParam_returns400() throws Exception {
        String accessToken = registerAndLogin("missingparam@example.com");

        mockMvc.perform(get("/games/search")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_withBlankQueryParam_returns400() throws Exception {
        String accessToken = registerAndLogin("blankparam@example.com");

        mockMvc.perform(get("/games/search")
                        .param("q", "  ")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_whenRawgClientFails_returns502() throws Exception {
        String accessToken = registerAndLogin("rawgdown@example.com");

        when(rawgClient.search(anyString())).thenThrow(new RawgUnavailableException("down"));

        mockMvc.perform(get("/games/search")
                        .param("q", "zelda")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadGateway());
    }

    private String registerAndLogin(String email) throws Exception {
        String password = "correct-horse-battery";

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentialsBody(email, password)))
                .andExpect(status().isCreated());

        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentialsBody(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(loginResponse).get("accessToken").asText();
    }

    private String credentialsBody(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(new CredentialsPayload(email, password));
    }

    private record CredentialsPayload(String email, String password) {
    }
}
