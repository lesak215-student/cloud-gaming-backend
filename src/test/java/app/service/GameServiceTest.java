package app.service;

import app.exception.BusinessRuleException;
import app.exception.ResourceNotFoundException;
import app.exception.ValidationException;
import app.model.Game;
import app.model.GamingSession;
import app.model.SessionStatus;
import app.model.SubscriptionTier;
import app.repository.GameRepository;
import app.repository.GamingSessionRepository;
import app.repository.InMemoryGameRepository;
import app.repository.InMemoryGamingSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit tests for GameService (without Spring Context)")
class GameServiceTest {

    private GameRepository gameRepository;
    private GamingSessionRepository sessionRepository;
    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameRepository = new InMemoryGameRepository();
        sessionRepository = new InMemoryGamingSessionRepository();
        gameService = new GameService(gameRepository, sessionRepository);
    }

    @Test
    @DisplayName("create() should create game successfully")
    void shouldCreateGameSuccessfully() {
        Game game = gameService.create("Cyberpunk 2077", "RPG", SubscriptionTier.PREMIUM, 5);

        assertNotNull(game.getId());
        assertEquals("Cyberpunk 2077", game.getTitle());
        assertEquals("RPG", game.getGenre());
        assertEquals(SubscriptionTier.PREMIUM, game.getRequiredTier());
        assertEquals(5, game.getMaxSlots());
    }

    @Test
    @DisplayName("create() should throw ValidationException on invalid game data")
    void shouldThrowValidationExceptionOnInvalidGameData() {
        assertThrows(ValidationException.class, () -> gameService.create(null, "RPG", SubscriptionTier.BASIC, 2));
        assertThrows(ValidationException.class, () -> gameService.create("   ", "RPG", SubscriptionTier.BASIC, 2));
        assertThrows(ValidationException.class, () -> gameService.create("Dota 2", null, SubscriptionTier.BASIC, 2));
        assertThrows(ValidationException.class, () -> gameService.create("Dota 2", "   ", SubscriptionTier.BASIC, 2));
        assertThrows(ValidationException.class, () -> gameService.create("Dota 2", "MOBA", null, 2));
        assertThrows(ValidationException.class, () -> gameService.create("Dota 2", "MOBA", SubscriptionTier.BASIC, 0));
        assertThrows(ValidationException.class, () -> gameService.create("Dota 2", "MOBA", SubscriptionTier.BASIC, -5));
    }

    @Test
    @DisplayName("create() should throw BusinessRuleException on duplicate title")
    void shouldThrowOnDuplicateTitle() {
        gameService.create("CS2", "Shooter", SubscriptionTier.BASIC, 2);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> gameService.create("cs2", "Shooter", SubscriptionTier.PREMIUM, 3));
        assertTrue(ex.getMessage().contains("Game with title already exists"));
    }

    @Test
    @DisplayName("getById() should return game or throw appropriate exception")
    void shouldGetByIdOrThrow() {
        Game created = gameService.create("Witcher 3", "RPG", SubscriptionTier.BASIC, 1);

        Game found = gameService.getById(created.getId());
        assertEquals(created.getId(), found.getId());

        assertThrows(ValidationException.class, () -> gameService.getById(null));
        assertThrows(ValidationException.class, () -> gameService.getById(0L));
        assertThrows(ValidationException.class, () -> gameService.getById(-1L));
        assertThrows(ResourceNotFoundException.class, () -> gameService.getById(999L));
    }

    @Test
    @DisplayName("getAll() should filter games by genre, tier and available slots")
    void shouldFilterGamesProperly() {
        Game g1 = gameService.create("CS2", "Shooter", SubscriptionTier.BASIC, 1);
        Game g2 = gameService.create("Apex Legends", "Shooter", SubscriptionTier.BASIC, 2);
        Game g3 = gameService.create("Cyberpunk 2077", "RPG", SubscriptionTier.PREMIUM, 2);

        assertEquals(3, gameService.getAll(null, null, null).size());

        List<Game> shooters = gameService.getAll("Shooter", null, null);
        assertEquals(2, shooters.size());

        List<Game> premiumGames = gameService.getAll(null, SubscriptionTier.PREMIUM, null);
        assertEquals(1, premiumGames.size());
        assertEquals("Cyberpunk 2077", premiumGames.getFirst().getTitle());

        // Occupy the only slot of CS2
        sessionRepository.save(new GamingSession(null, 1L, g1.getId(), Instant.now(), null, 1080, SessionStatus.ACTIVE));

        List<Game> gamesWithSlots = gameService.getAll(null, null, true);
        assertEquals(2, gamesWithSlots.size());
        assertFalse(gamesWithSlots.stream().anyMatch(g -> g.getId().equals(g1.getId())));

        List<Game> gamesWithoutSlots = gameService.getAll(null, null, false);
        assertEquals(1, gamesWithoutSlots.size());
        assertEquals(g1.getId(), gamesWithoutSlots.getFirst().getId());
    }

    @Test
    @DisplayName("update() should update game fields and validate duplicates")
    void shouldUpdateGameSuccessfully() {
        Game game = gameService.create("CS", "Shooter", SubscriptionTier.BASIC, 2);

        Game updated = gameService.update(game.getId(), "Counter-Strike 2", "FPS", SubscriptionTier.BASIC, 4);

        assertEquals("Counter-Strike 2", updated.getTitle());
        assertEquals("FPS", updated.getGenre());
        assertEquals(4, updated.getMaxSlots());
    }

    @Test
    @DisplayName("update() should prevent decreasing maxSlots below active sessions count")
    void shouldPreventDecreasingMaxSlotsBelowActiveSessions() {
        Game game = gameService.create("CS2", "Shooter", SubscriptionTier.BASIC, 3);
        sessionRepository.save(new GamingSession(null, 1L, game.getId(), Instant.now(), null, 1080, SessionStatus.ACTIVE));
        sessionRepository.save(new GamingSession(null, 2L, game.getId(), Instant.now(), null, 1080, SessionStatus.PAUSED));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> gameService.update(game.getId(), "CS2", "Shooter", SubscriptionTier.BASIC, 1));
        assertTrue(ex.getMessage().contains("Cannot decrease max slots below current active sessions count"));

        assertDoesNotThrow(() -> gameService.update(game.getId(), "CS2", "Shooter", SubscriptionTier.BASIC, 2));
    }

    @Test
    @DisplayName("update() should prevent changing requiredTier when active or paused sessions exist")
    void shouldPreventChangingRequiredTierWhenActiveSessionsExist() {
        Game game = gameService.create("Dota 2", "MOBA", SubscriptionTier.BASIC, 5);
        sessionRepository.save(new GamingSession(null, 1L, game.getId(), Instant.now(), null, 1080, SessionStatus.ACTIVE));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> gameService.update(game.getId(), "Dota 2", "MOBA", SubscriptionTier.PREMIUM, 5));
        assertTrue(ex.getMessage().contains("Cannot change required tier of game that has active or paused sessions"));
    }

    @Test
    @DisplayName("delete() should remove game or throw conflict if active session exists")
    void shouldDeleteGameOrThrowConflict() {
        Game game = gameService.create("Portal", "Puzzle", SubscriptionTier.BASIC, 1);
        Long id = game.getId();

        gameService.delete(id);
        assertThrows(ResourceNotFoundException.class, () -> gameService.getById(id));

        Game activeGame = gameService.create("Portal 2", "Puzzle", SubscriptionTier.BASIC, 1);
        sessionRepository.save(new GamingSession(null, 1L, activeGame.getId(), Instant.now(), null, 1080, SessionStatus.ACTIVE));

        assertThrows(BusinessRuleException.class, () -> gameService.delete(activeGame.getId()));
    }
}
