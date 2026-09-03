package com.tov.gamelogger.repository;

import com.tov.gamelogger.domain.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {

    Optional<Game> findByExternalId(String externalId);
}
