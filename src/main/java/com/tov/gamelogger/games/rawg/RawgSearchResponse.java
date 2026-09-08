package com.tov.gamelogger.games.rawg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record RawgSearchResponse(List<RawgGameSummary> results) {
}
