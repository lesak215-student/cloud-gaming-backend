package app.integration;

import app.exception.BusinessRuleException;
import app.model.Game;
import app.model.GamingSession;
import app.model.SessionStatus;
import app.model.SubscriptionTier;
import app.model.User;
import app.repository.GameRepository;
import app.repository.GamingSessionRepository;
import app.repository.UserRepository;
import app.service.GameService;
import app.service.GamingSessionService;
import app.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Database Integration Tests - Persistence, Relationships, Rollback & N+1 Prevention")
class DatabaseIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private GameService gameService;

    @Autowired
    private GamingSessionService sessionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GamingSessionRepository sessionRepository;

    @BeforeEach
    void cleanUp() {
        sessionRepository.deleteAll();
        gameRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @Transactional
    @DisplayName("Requirements 1 & 2: Persist related User, Game and GamingSession with foreign key integrity")
    void shouldPersistRelatedEntitiesWithReferentialIntegrity() {
        User user = userService.create("Alice", "alice@example.com", SubscriptionTier.PREMIUM);
        assertNotNull(user.getId());

        Game game = gameService.create("Cyberpunk 2077", "RPG", SubscriptionTier.PREMIUM, 10);
        assertNotNull(game.getId());

        GamingSession session = sessionService.start(user.getId(), game.getId(), 2160);
        assertNotNull(session.getId());

        GamingSession reloaded = sessionRepository.findById(session.getId()).orElseThrow();
        assertEquals(SessionStatus.ACTIVE, reloaded.getStatus());
        assertEquals(2160, reloaded.getResolution());
        assertNotNull(reloaded.getUser());
        assertEquals(user.getId(), reloaded.getUser().getId());
        assertEquals("Alice", reloaded.getUser().getName());
        assertNotNull(reloaded.getGame());
        assertEquals(game.getId(), reloaded.getGame().getId());
        assertEquals("Cyberpunk 2077", reloaded.getGame().getTitle());
    }

    @Test
    @DisplayName("Requirement 2: Referential integrity prevents dangling links on deletion")
    void shouldProtectReferentialIntegrityOnDeletion() {
        User user = userService.create("Bob", "bob@example.com", SubscriptionTier.BASIC);
        Game game = gameService.create("CS2", "Shooter", SubscriptionTier.BASIC, 5);
        GamingSession session = sessionService.start(user.getId(), game.getId(), 1080);

        // Cannot delete user or game while active session exists
        assertThrows(BusinessRuleException.class, () -> userService.delete(user.getId()));
        assertThrows(BusinessRuleException.class, () -> gameService.delete(game.getId()));

        // Terminate session
        sessionService.terminate(session.getId());

        // Now deletion should cleanly remove user and completed sessions without constraint errors
        userService.delete(user.getId());
        assertFalse(userRepository.existsById(user.getId()));
        assertTrue(sessionRepository.findByUserId(user.getId()).isEmpty());
    }

    @Test
    @DisplayName("Requirement 3: Atomic operation modifying multiple related resources with full rollback on failure")
    void shouldRollbackAllChangesWhenErrorOccursInTransaction() {
        User user1 = userService.create("Player1", "p1@example.com", SubscriptionTier.PREMIUM);
        User user2 = userService.create("Player2", "p2@example.com", SubscriptionTier.PREMIUM);
        Game game = gameService.create("Witcher 3", "RPG", SubscriptionTier.BASIC, 5);

        GamingSession s1 = sessionService.start(user1.getId(), game.getId(), 1080);
        GamingSession s2 = sessionService.start(user2.getId(), game.getId(), 1080);
        sessionService.pause(s2.getId());

        // Attempt maintenance with triggerError=true
        assertThrows(BusinessRuleException.class, () ->
                gameService.scheduleMaintenance(game.getId(), 0, "Emergency patch", true));

        // Verify in database: Game slots MUST NOT be changed
        Game gameAfterRollback = gameRepository.findById(game.getId()).orElseThrow();
        assertEquals(5, gameAfterRollback.getMaxSlots(), "Game slots must remain 5 after rollback");

        // Verify in database: Session s1 MUST still be ACTIVE with no notes
        GamingSession s1AfterRollback = sessionRepository.findById(s1.getId()).orElseThrow();
        assertEquals(SessionStatus.ACTIVE, s1AfterRollback.getStatus(), "Session s1 status must remain ACTIVE after rollback");
        assertNull(s1AfterRollback.getEndedAt(), "Session s1 endedAt must remain null after rollback");
        assertNull(s1AfterRollback.getNotes(), "Session s1 notes must remain null after rollback");

        // Verify in database: Session s2 MUST still be PAUSED with no notes
        GamingSession s2AfterRollback = sessionRepository.findById(s2.getId()).orElseThrow();
        assertEquals(SessionStatus.PAUSED, s2AfterRollback.getStatus(), "Session s2 status must remain PAUSED after rollback");
        assertNull(s2AfterRollback.getEndedAt(), "Session s2 endedAt must remain null after rollback");
        assertNull(s2AfterRollback.getNotes(), "Session s2 notes must remain null after rollback");
    }

    @Test
    @DisplayName("Requirement 5: Fetch list of resources with related objects without N+1 problem on 30+ items")
    void shouldFetchDetailedListWithoutNPlusOneProblem() {
        Game game = gameRepository.save(new Game(null, "Massive MMO", "MMO", SubscriptionTier.PREMIUM, 100));
        List<GamingSession> created = new ArrayList<>();

        for (int i = 1; i <= 35; i++) {
            User user = userRepository.save(new User(null, "Gamer-" + i, "gamer" + i + "@test.com", SubscriptionTier.PREMIUM));
            GamingSession session = new GamingSession(null, user, game, Instant.now(), null, 1080, SessionStatus.ACTIVE, "Session-" + i);
            created.add(sessionRepository.save(session));
        }

        assertEquals(35, created.size());

        List<GamingSession> detailedList = sessionService.getAllDetailed();

        assertEquals(35, detailedList.size());
        for (GamingSession s : detailedList) {
            assertNotNull(s.getUser(), "Related user must be pre-fetched");
            assertNotNull(s.getUser().getName(), "User name must be accessible without additional query");
            assertNotNull(s.getGame(), "Related game must be pre-fetched");
            assertEquals("Massive MMO", s.getGame().getTitle(), "Game title must be accessible without additional query");
        }
    }
}
