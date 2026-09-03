package com.tov.gamelogger.domain;

import com.tov.gamelogger.repository.GameRepository;
import com.tov.gamelogger.repository.LibraryEntryRepository;
import com.tov.gamelogger.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SpringBootTest
@Testcontainers
@Transactional
class LibraryPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    UserRepository userRepository;

    @Autowired
    GameRepository gameRepository;

    @Autowired
    LibraryEntryRepository libraryEntryRepository;

    @Test
    void persistsGameAndRejectsDuplicateExternalId() {
        gameRepository.saveAndFlush(new Game("steam:440", "Team Fortress 2"));

        Game duplicate = new Game("steam:440", "Team Fortress 2 (dup)");
        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> gameRepository.saveAndFlush(duplicate));
    }

    @Test
    void persistsLibraryEntryAndRejectsDuplicateUserGamePair() {
        User user = userRepository.saveAndFlush(new User("alice"));
        Game game = gameRepository.saveAndFlush(new Game("steam:70", "Half-Life"));
        libraryEntryRepository.saveAndFlush(new LibraryEntry(user, game));

        LibraryEntry duplicate = new LibraryEntry(user, game);
        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> libraryEntryRepository.saveAndFlush(duplicate));
    }

    @Test
    void roundTripsUserGameAndLibraryEntry() {
        User user = userRepository.saveAndFlush(new User("bob"));
        Game game = gameRepository.saveAndFlush(new Game("steam:400", "Portal"));
        game.getGenres().add("puzzle");
        game.getTags().add("singleplayer");
        gameRepository.saveAndFlush(game);

        LibraryEntry entry = new LibraryEntry(user, game);
        entry.setTotalPlaytimeMinutes(180);
        LibraryEntry saved = libraryEntryRepository.saveAndFlush(entry);

        LibraryEntry reloaded = libraryEntryRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getUser().getUsername()).isEqualTo("bob");
        assertThat(reloaded.getGame().getExternalId()).isEqualTo("steam:400");
        assertThat(reloaded.getGame().getGenres()).containsExactly("puzzle");
        assertThat(reloaded.getStatus()).isEqualTo(LibraryStatus.BACKLOG);
        assertThat(reloaded.getTotalPlaytimeMinutes()).isEqualTo(180);
    }
}
