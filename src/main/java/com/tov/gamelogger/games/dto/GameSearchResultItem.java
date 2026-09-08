package com.tov.gamelogger.games.dto;

import java.time.LocalDate;
import java.util.List;

public record GameSearchResultItem(
        String externalId,
        String name,
        LocalDate releaseDate,
        String coverImageUrl,
        List<String> genres) {
}
