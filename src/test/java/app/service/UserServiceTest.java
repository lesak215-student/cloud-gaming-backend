package app.service;

import app.exception.BusinessRuleException;
import app.exception.ResourceNotFoundException;
import app.exception.ValidationException;
import app.model.GamingSession;
import app.model.PremiumUser;
import app.model.SessionStatus;
import app.model.StandardUser;
import app.model.SubscriptionTier;
import app.model.User;
import app.repository.GamingSessionRepository;
import app.repository.InMemoryGamingSessionRepository;
import app.repository.InMemoryUserRepository;
import app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit tests for UserService (without Spring Context)")
class UserServiceTest {

    private UserRepository userRepository;
    private GamingSessionRepository sessionRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        sessionRepository = new InMemoryGamingSessionRepository();
        userService = new UserService(userRepository, sessionRepository);
    }

    @Test
    @DisplayName("create() should create StandardUser when tier is BASIC")
    void shouldCreateStandardUser() {
        User user = userService.create("Alice", "alice@example.com", SubscriptionTier.BASIC);

        assertNotNull(user.getId());
        assertEquals("Alice", user.getName());
        assertEquals("alice@example.com", user.getEmail());
        assertEquals(SubscriptionTier.BASIC, user.getSubscriptionTier());
        assertInstanceOf(StandardUser.class, user);
        assertEquals(1, user.getMaxConcurrentSessions());
        assertEquals(1080, user.getMaxStreamingResolution());
    }

    @Test
    @DisplayName("create() should create PremiumUser when tier is PREMIUM")
    void shouldCreatePremiumUser() {
        User user = userService.create("Bob", "bob@example.com", SubscriptionTier.PREMIUM);

        assertNotNull(user.getId());
        assertEquals("Bob", user.getName());
        assertEquals("bob@example.com", user.getEmail());
        assertEquals(SubscriptionTier.PREMIUM, user.getSubscriptionTier());
        assertInstanceOf(PremiumUser.class, user);
        assertEquals(3, user.getMaxConcurrentSessions());
        assertEquals(2160, user.getMaxStreamingResolution());
    }

    @Test
    @DisplayName("create() should throw ValidationException on invalid user data")
    void shouldThrowValidationExceptionOnInvalidUserData() {
        assertThrows(ValidationException.class, () -> userService.create(null, "valid@email.com", SubscriptionTier.BASIC));
        assertThrows(ValidationException.class, () -> userService.create("   ", "valid@email.com", SubscriptionTier.BASIC));
        assertThrows(ValidationException.class, () -> userService.create("Alice", null, SubscriptionTier.BASIC));
        assertThrows(ValidationException.class, () -> userService.create("Alice", "invalid-email-without-at", SubscriptionTier.BASIC));
        assertThrows(ValidationException.class, () -> userService.create("Alice", "valid@email.com", null));
    }

    @Test
    @DisplayName("create() should throw BusinessRuleException on duplicate email")
    void shouldThrowBusinessRuleExceptionOnDuplicateEmail() {
        userService.create("Alice", "alice@example.com", SubscriptionTier.BASIC);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> userService.create("Alice Copy", "ALICE@EXAMPLE.COM", SubscriptionTier.BASIC));
        assertTrue(ex.getMessage().contains("User with email already exists"));
    }

    @Test
    @DisplayName("getById() should return user or throw appropriate exceptions")
    void shouldGetByIdOrThrow() {
        User created = userService.create("Alice", "alice@example.com", SubscriptionTier.BASIC);

        User found = userService.getById(created.getId());
        assertEquals(created.getId(), found.getId());

        assertThrows(ValidationException.class, () -> userService.getById(null));
        assertThrows(ValidationException.class, () -> userService.getById(0L));
        assertThrows(ValidationException.class, () -> userService.getById(-1L));
        assertThrows(ResourceNotFoundException.class, () -> userService.getById(999L));
    }

    @Test
    @DisplayName("getAll() should return all users or filter by tier")
    void shouldGetAllUsersWithTierFilter() {
        userService.create("Alice", "alice@example.com", SubscriptionTier.BASIC);
        userService.create("Bob", "bob@example.com", SubscriptionTier.PREMIUM);
        userService.create("Charlie", "charlie@example.com", SubscriptionTier.BASIC);

        assertEquals(3, userService.getAll(null).size());

        List<User> basicUsers = userService.getAll(SubscriptionTier.BASIC);
        assertEquals(2, basicUsers.size());
        assertTrue(basicUsers.stream().allMatch(u -> u.getSubscriptionTier() == SubscriptionTier.BASIC));

        List<User> premiumUsers = userService.getAll(SubscriptionTier.PREMIUM);
        assertEquals(1, premiumUsers.size());
        assertEquals("Bob", premiumUsers.getFirst().getName());
    }

    @Test
    @DisplayName("update() should update user details successfully")
    void shouldUpdateUserSuccessfully() {
        User user = userService.create("Alice", "alice@example.com", SubscriptionTier.BASIC);

        User updated = userService.update(user.getId(), "Alice Smith", "alice.smith@example.com", SubscriptionTier.PREMIUM);

        assertEquals(user.getId(), updated.getId());
        assertEquals("Alice Smith", updated.getName());
        assertEquals("alice.smith@example.com", updated.getEmail());
        assertEquals(SubscriptionTier.PREMIUM, updated.getSubscriptionTier());
        assertInstanceOf(PremiumUser.class, updated);
    }

    @Test
    @DisplayName("update() should allow keeping the same email for the same user")
    void shouldAllowSameEmailOnUpdate() {
        User user = userService.create("Alice", "alice@example.com", SubscriptionTier.BASIC);
        assertDoesNotThrow(() -> userService.update(user.getId(), "Alice Updated", "alice@example.com", SubscriptionTier.BASIC));
    }

    @Test
    @DisplayName("update() should throw BusinessRuleException on duplicate email with another user")
    void shouldThrowOnDuplicateEmailWithAnotherUser() {
        userService.create("Alice", "alice@example.com", SubscriptionTier.BASIC);
        User bob = userService.create("Bob", "bob@example.com", SubscriptionTier.PREMIUM);

        assertThrows(BusinessRuleException.class,
                () -> userService.update(bob.getId(), "Bob Updated", "alice@example.com", SubscriptionTier.PREMIUM));
    }

    @Test
    @DisplayName("update() should prevent tier change when user has active or paused session")
    void shouldPreventTierChangeWhenUserHasActiveOrPausedSession() {
        User user = userService.create("Bob", "bob@example.com", SubscriptionTier.PREMIUM);
        sessionRepository.save(new GamingSession(null, user.getId(), 10L, Instant.now(), null, 2160, SessionStatus.ACTIVE));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> userService.update(user.getId(), "Bob", "bob@example.com", SubscriptionTier.BASIC));
        assertTrue(ex.getMessage().contains("Cannot change subscription tier while user has an active or paused session"));

        assertDoesNotThrow(() -> userService.update(user.getId(), "Bob Updated", "bob@example.com", SubscriptionTier.PREMIUM));
    }

    @Test
    @DisplayName("delete() should remove user when no active sessions")
    void shouldDeleteUserSuccessfully() {
        User user = userService.create("Alice", "alice@example.com", SubscriptionTier.BASIC);
        Long id = user.getId();

        userService.delete(id);

        assertThrows(ResourceNotFoundException.class, () -> userService.getById(id));
    }

    @Test
    @DisplayName("delete() should throw when user does not exist or id invalid")
    void shouldThrowOnDeleteMissingOrInvalid() {
        assertThrows(ValidationException.class, () -> userService.delete(0L));
        assertThrows(ResourceNotFoundException.class, () -> userService.delete(999L));
    }

    @Test
    @DisplayName("delete() should throw BusinessRuleException when user has active or paused session")
    void shouldThrowConflictWhenDeletingUserWithActiveSession() {
        User user = userService.create("Alice", "alice@example.com", SubscriptionTier.BASIC);
        sessionRepository.save(new GamingSession(null, user.getId(), 10L, Instant.now(), null, 1080, SessionStatus.ACTIVE));

        assertThrows(BusinessRuleException.class, () -> userService.delete(user.getId()));
    }
}
