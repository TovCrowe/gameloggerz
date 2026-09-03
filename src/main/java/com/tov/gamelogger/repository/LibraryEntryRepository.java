package com.tov.gamelogger.repository;

import com.tov.gamelogger.domain.LibraryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LibraryEntryRepository extends JpaRepository<LibraryEntry, Long> {

    Optional<LibraryEntry> findByUserIdAndGameId(Long userId, Long gameId);

    boolean existsByUserIdAndGameId(Long userId, Long gameId);

    List<LibraryEntry> findByUserId(Long userId);
}
