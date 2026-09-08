package com.tov.gamelogger.games;

import com.tov.gamelogger.games.dto.GameSearchResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/games")
public class GameSearchController {

    private final GameSearchService gameSearchService;

    public GameSearchController(GameSearchService gameSearchService) {
        this.gameSearchService = gameSearchService;
    }

    @GetMapping("/search")
    public GameSearchResponse search(@RequestParam @NotBlank String q) {
        return new GameSearchResponse(gameSearchService.search(q));
    }
}
