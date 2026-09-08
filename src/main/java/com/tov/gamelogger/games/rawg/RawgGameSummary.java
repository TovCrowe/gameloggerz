package com.tov.gamelogger.games.rawg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record RawgGameSummary(
        Long id,
        String name,
        String released,
        @JsonProperty("background_image") String backgroundImage,
        List<RawgGenre> genres) {
}
