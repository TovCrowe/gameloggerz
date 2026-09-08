package com.tov.gamelogger.games.rawg;

import com.tov.gamelogger.games.RawgUnavailableException;
import com.tov.gamelogger.games.dto.GameSearchResultItem;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RawgHttpClientTest {

    private static final String BASE_URL = "https://api.rawg.io/api";

    @Test
    void search_mapsRawgPayloadToDto() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        RawgHttpClient client = new RawgHttpClient(restClient, "test-key");

        String body = """
                {
                  "results": [
                    {
                      "id": 3498,
                      "name": "Grand Theft Auto V",
                      "released": "2013-09-17",
                      "background_image": "https://example.com/gta5.jpg",
                      "genres": [{"id": 4, "name": "Action"}, {"id": 3, "name": "Adventure"}]
                    }
                  ]
                }
                """;

        server.expect(requestTo(org.hamcrest.Matchers.containsString("/games")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        List<GameSearchResultItem> results = client.search("gta");

        server.verify();
        assertThat(results).hasSize(1);
        GameSearchResultItem item = results.get(0);
        assertThat(item.externalId()).isEqualTo("3498");
        assertThat(item.name()).isEqualTo("Grand Theft Auto V");
        assertThat(item.releaseDate()).isEqualTo(LocalDate.of(2013, 9, 17));
        assertThat(item.coverImageUrl()).isEqualTo("https://example.com/gta5.jpg");
        assertThat(item.genres()).containsExactly("Action", "Adventure");
    }

    @Test
    void search_whenRawgReturns5xx_throwsRawgUnavailableException() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        RawgHttpClient client = new RawgHttpClient(restClient, "test-key");

        server.expect(requestTo(org.hamcrest.Matchers.containsString("/games")))
                .andRespond(withServerError());

        assertThatExceptionOfType(RawgUnavailableException.class)
                .isThrownBy(() -> client.search("gta"));
    }

    @Test
    void search_withBlankApiKey_throwsRawgUnavailableExceptionWithoutCallingRawg() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        RawgHttpClient client = new RawgHttpClient(restClient, "");

        assertThatExceptionOfType(RawgUnavailableException.class)
                .isThrownBy(() -> client.search("gta"));

        server.verify();
    }
}
