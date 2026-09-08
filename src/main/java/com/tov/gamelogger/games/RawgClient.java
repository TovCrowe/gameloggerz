package com.tov.gamelogger.games;

import com.tov.gamelogger.games.dto.GameSearchResultItem;

import java.util.List;

public interface RawgClient {

    List<GameSearchResultItem> search(String query);
}
