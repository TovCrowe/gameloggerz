package com.tov.gamelogger.games;

import com.tov.gamelogger.games.dto.GameSearchResultItem;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GameSearchService {

    private final RawgClient rawgClient;

    public GameSearchService(RawgClient rawgClient) {
        this.rawgClient = rawgClient;
    }

    public List<GameSearchResultItem> search(String query) {
        return rawgClient.search(query);
    }
}
