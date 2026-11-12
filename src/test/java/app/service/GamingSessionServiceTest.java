package app.service;

import app.exception.BusinessRuleException;
import app.exception.ResourceNotFoundException;
import app.exception.ValidationException;
import app.model.Game;
import app.model.GamingSession;
import app.model.PremiumUser;
import app.model.SessionStatus;
import app.model.StandardUser;
import app.model.SubscriptionTier;
import app.model.User;
import app.repository.GameRepository;
import app.repository.GamingSessionRepository;
import app.repository.InMemoryGameRepository;
import app.repository.InMemoryGamingSessionRepository;
import app.repository.InMemoryUserRepository;
import app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit tests for GamingSessionService (without Spring Context)")
class GamingSessionServiceTest {

    private GamingSessionRepository sessionRepository;
    private UserRepository userRepository;
    private GameRepository gameRepository;
    private GamingSessionService sessionService;

    private User standardUser;
    private User premiumUser;
    private Game basicGame;
    private Game premiumGame;

    @BeforeEach
    void setUp() {
        sessionRepository = new InMemoryGamingSessionRepository();
        userRepository = new InMemoryUserRepository();
        gameRepository = new InMemoryGameRepository();
        sessionService = new GamingSessionService(sessionRepository, userRepository, gameRepository);

        standardUser = userRepository.save(new StandardUser(null, "Alice", "alice@example.com"));
        premiumUser = userRepository.save(new PremiumUser(null, "Bob", "bob@example.com"));

        basicGame = gameRepository.save(new Game(null, "CS2", "Shooter", SubscriptionTier.BASIC, 2));
        premiumGame = gameRepository.save(new Game(null, "Cyberpunk", "RPG", SubscriptionTier.PREMIUM, 1));
    }

    @Test
    @DisplayName("start() should create active session for valid user and game")
    void shouldStartSessionSuccessfully() {
        GamingSession session = sessionService.start(standardUser.getId(), basicGame.getId(), 720);

        assertNotNull(session.getId());
        assertEquals(standardUser.getId(), session.getUserId());
        assertEquals(basicGame.getId(), session.getGameId());
        assertEquals(720, session.getResolution());
        assertEquals(SessionStatus.ACTIVE, session.getStatus());
        assertNotNull(session.getStartedAt());
        assertNull(session.getEndedAt());
    }

    @Test
    @DisplayName("start() with null resolution should default to user tier max resolution")
    void shouldDefaultResolutionToUserTierMax() {
        GamingSession session1 = sessionService.start(standardUser.getId(), basicGame.getId(), null);
        assertEquals(1080, session1.getResolution());

        GamingSession session2 = sessionService.start(premiumUser.getId(), premiumGame.getId(), null);
        assertEquals(2160, session2.getResolution());
    }

    @Test
    @DisplayName("start() should throw ValidationException on negative or zero resolution")
    void shouldThrowValidationExceptionOnNegativeOrZeroResolution() {
        ValidationException exNegative = assertThrows(ValidationException.class,
                () -> sessionService.start(standardUser.getId(), basicGame.getId(), -100));
        assertEquals("Resolution must be a positive integer", exNegative.getMessage());

        ValidationException exZero = assertThrows(ValidationException.class,
                () -> sessionService.start(standardUser.getId(), basicGame.getId(), 0));
        assertEquals("Resolution must be a positive integer", exZero.getMessage());
    }

    @Test
    @DisplayName("start() should throw ValidationException on invalid user or game id")
    void shouldThrowOnInvalidIds() {
        assertThrows(ValidationException.class, () -> sessionService.start(null, basicGame.getId(), 1080));
        assertThrows(ValidationException.class, () -> sessionService.start(0L, basicGame.getId(), 1080));
        assertThrows(ValidationException.class, () -> sessionService.start(-5L, basicGame.getId(), 1080));
        assertThrows(ValidationException.class, () -> sessionService.start(standardUser.getId(), null, 1080));
        assertThrows(ValidationException.class, () -> sessionService.start(standardUser.getId(), 0L, 1080));
    }

    @Test
    @DisplayName("start() should throw ResourceNotFoundException when user or game not found")
    void shouldThrowNotFoundWhenUserOrGameMissing() {
        assertThrows(ResourceNotFoundException.class, () -> sessionService.start(999L, basicGame.getId(), 1080));
        assertThrows(ResourceNotFoundException.class, () -> sessionService.start(standardUser.getId(), 999L, 1080));
    }

    @Test
    @DisplayName("start() should throw BusinessRuleException when resolution exceeds user tier")
    void shouldThrowWhenResolutionExceedsTierLimit() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> sessionService.start(standardUser.getId(), basicGame.getId(), 1440));
        assertTrue(ex.getMessage().contains("exceeds maximum allowed for user tier"));
    }

    @Test
    @DisplayName("start() should prevent StandardUser from launching PREMIUM game")
    void shouldPreventStandardUserFromPlayingPremiumGame() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> sessionService.start(standardUser.getId(), premiumGame.getId(), 1080));
        assertTrue(ex.getMessage().contains("does not allow playing game with required tier"));
    }

    @Test
    @DisplayName("start() should enforce concurrent sessions limit per user")
    void shouldEnforceUserConcurrentSessionsLimit() {
        sessionService.start(standardUser.getId(), basicGame.getId(), 1080);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> sessionService.start(standardUser.getId(), basicGame.getId(), 720));
        assertTrue(ex.getMessage().contains("User has reached maximum concurrent sessions limit"));

        // Premium user limit is 3
        Game g2 = gameRepository.save(new Game(null, "Game 2", "Action", SubscriptionTier.BASIC, 5));
        Game g3 = gameRepository.save(new Game(null, "Game 3", "Action", SubscriptionTier.BASIC, 5));
        Game g4 = gameRepository.save(new Game(null, "Game 4", "Action", SubscriptionTier.BASIC, 5));

        sessionService.start(premiumUser.getId(), basicGame.getId(), 1080);
        sessionService.start(premiumUser.getId(), g2.getId(), 1080);
        sessionService.start(premiumUser.getId(), g3.getId(), 1080);

        assertThrows(BusinessRuleException.class,
                () -> sessionService.start(premiumUser.getId(), g4.getId(), 1080));
    }

    @Test
    @DisplayName("start() should enforce max slots limit per game")
    void shouldEnforceGameSlotsLimit() {
        sessionService.start(premiumUser.getId(), premiumGame.getId(), 1080);

        User premium2 = userRepository.save(new PremiumUser(null, "Carol", "carol@example.com"));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> sessionService.start(premium2.getId(), premiumGame.getId(), 1080));
        assertTrue(ex.getMessage().contains("slots are occupied for game"));
    }

    @Test
    @DisplayName("pause() and resume() should transition session states properly")
    void shouldPauseAndResumeSession() {
        GamingSession session = sessionService.start(standardUser.getId(), basicGame.getId(), 1080);

        GamingSession paused = sessionService.pause(session.getId());
        assertEquals(SessionStatus.PAUSED, paused.getStatus());
        assertTrue(paused.isPaused());

        // Pausing already paused session should fail
        assertThrows(BusinessRuleException.class, () -> sessionService.pause(session.getId()));

        GamingSession resumed = sessionService.resume(session.getId());
        assertEquals(SessionStatus.ACTIVE, resumed.getStatus());
        assertTrue(resumed.isActive());

        // Resuming already active session should fail
        assertThrows(BusinessRuleException.class, () -> sessionService.resume(session.getId()));
    }

    @Test
    @DisplayName("terminate() should complete active or paused session and set endedAt")
    void shouldTerminateSession() {
        GamingSession session = sessionService.start(standardUser.getId(), basicGame.getId(), 1080);

        GamingSession terminated = sessionService.terminate(session.getId());
        assertEquals(SessionStatus.COMPLETED, terminated.getStatus());
        assertTrue(terminated.isCompleted());
        assertNotNull(terminated.getEndedAt());

        // Terminating already completed session should fail
        assertThrows(BusinessRuleException.class, () -> sessionService.terminate(session.getId()));
    }

    @Test
    @DisplayName("update() should update resolution or throw validation/conflict")
    void shouldUpdateResolution() {
        GamingSession session = sessionService.start(standardUser.getId(), basicGame.getId(), 720);

        GamingSession updated = sessionService.update(session.getId(), 1080);
        assertEquals(1080, updated.getResolution());

        // Exceeds tier
        assertThrows(BusinessRuleException.class, () -> sessionService.update(session.getId(), 2160));

        // Invalid resolution
        assertThrows(ValidationException.class, () -> sessionService.update(session.getId(), 0));
        assertThrows(ValidationException.class, () -> sessionService.update(session.getId(), -50));
        assertThrows(ValidationException.class, () -> sessionService.update(session.getId(), null));

        // Cannot modify completed session
        sessionService.terminate(session.getId());
        assertThrows(BusinessRuleException.class, () -> sessionService.update(session.getId(), 720));
    }

    @Test
    @DisplayName("delete() should only delete COMPLETED session")
    void shouldDeleteCompletedSession() {
        GamingSession session = sessionService.start(standardUser.getId(), basicGame.getId(), 1080);

        // Cannot delete active session
        assertThrows(BusinessRuleException.class, () -> sessionService.delete(session.getId()));

        sessionService.pause(session.getId());
        // Cannot delete paused session
        assertThrows(BusinessRuleException.class, () -> sessionService.delete(session.getId()));

        sessionService.terminate(session.getId());
        assertDoesNotThrow(() -> sessionService.delete(session.getId()));

        assertThrows(ResourceNotFoundException.class, () -> sessionService.getById(session.getId()));
    }

    @Test
    @DisplayName("getAll() should filter sessions by user, game and status")
    void shouldFilterSessions() {
        GamingSession s1 = sessionService.start(standardUser.getId(), basicGame.getId(), 1080);
        GamingSession s2 = sessionService.start(premiumUser.getId(), basicGame.getId(), 1080);
        sessionService.pause(s2.getId());

        assertEquals(2, sessionService.getAll(null, null, null).size());
        assertEquals(1, sessionService.getAll(standardUser.getId(), null, null).size());
        assertEquals(2, sessionService.getAll(null, basicGame.getId(), null).size());
        assertEquals(1, sessionService.getAll(null, null, SessionStatus.ACTIVE).size());
        assertEquals(1, sessionService.getAll(null, null, SessionStatus.PAUSED).size());
        assertEquals(0, sessionService.getAll(null, null, SessionStatus.COMPLETED).size());
    }
}
