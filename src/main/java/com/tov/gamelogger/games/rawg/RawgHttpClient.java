package com.tov.gamelogger.games.rawg;

import com.tov.gamelogger.games.RawgClient;
import com.tov.gamelogger.games.RawgUnavailableException;
import com.tov.gamelogger.games.dto.GameSearchResultItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Component
public class RawgHttpClient implements RawgClient {

    private static final int PAGE_SIZE = 20;

    private final RestClient restClient;
    private final String apiKey;

    public RawgHttpClient(RestClient rawgRestClient, @Value("${rawg.api-key}") String rawgApiKey) {
        this.restClient = rawgRestClient;
        this.apiKey = rawgApiKey;
    }

    @Override
    public List<GameSearchResultItem> search(String query) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RawgUnavailableException("RAWG API key is not configured");
        }

        RawgSearchResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/games")
                            .queryParam("key", apiKey)
                            .queryParam("search", query)
                            .queryParam("page_size", PAGE_SIZE)
                            .build())
                    .retrieve()
                    .body(RawgSearchResponse.class);
        } catch (RestClientException ex) {
            throw new RawgUnavailableException("RAWG search request failed", ex);
        }

        if (response == null || response.results() == null) {
            return List.of();
        }

        return response.results().stream()
                .map(this::toResultItem)
                .toList();
    }

    private GameSearchResultItem toResultItem(RawgGameSummary summary) {
        return new GameSearchResultItem(
                String.valueOf(summary.id()),
                summary.name(),
                parseReleaseDate(summary.released()),
                summary.backgroundImage(),
                mapGenres(summary.genres()));
    }

    private LocalDate parseReleaseDate(String released) {
        if (released == null || released.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(released);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private List<String> mapGenres(List<RawgGenre> genres) {
        if (genres == null) {
            return List.of();
        }
        return genres.stream().map(RawgGenre::name).toList();
    }
}
