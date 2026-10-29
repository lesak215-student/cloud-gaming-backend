package app.service;

import app.exception.BusinessRuleException;
import app.exception.ResourceNotFoundException;
import app.exception.ValidationException;
import app.model.PremiumUser;
import app.model.StandardUser;
import app.model.SubscriptionTier;
import app.model.User;
import app.repository.GamingSessionRepository;
import app.repository.UserRepository;

import java.util.List;

public class UserService {
    private final UserRepository userRepository;
    private final GamingSessionRepository sessionRepository;

    public UserService(UserRepository userRepository, GamingSessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
    }

    public synchronized User create(String name, String email, SubscriptionTier tier) {
        validateUserData(name, email, tier);
        String trimmedEmail = email.trim();
        if (userRepository.findByEmail(trimmedEmail).isPresent()) {
            throw new BusinessRuleException("User with email already exists: " + trimmedEmail);
        }
        User user = createUserInstance(null, name.trim(), trimmedEmail, tier);
        return userRepository.save(user);
    }

    public synchronized User update(Long id, String name, String email, SubscriptionTier tier) {
        if (id == null || id <= 0) {
            throw new ValidationException("User id must be a positive integer");
        }
        User existing = getById(id);
        validateUserData(name, email, tier);

        String trimmedEmail = email.trim();
        userRepository.findByEmail(trimmedEmail).ifPresent(userWithSameEmail -> {
            if (!userWithSameEmail.getId().equals(id)) {
                throw new BusinessRuleException("Email is already taken by another user: " + trimmedEmail);
            }
        });

        if (sessionRepository.hasActiveOrPausedSessionForUser(id) && tier != existing.getSubscriptionTier()) {
            throw new BusinessRuleException("Cannot change subscription tier while user has an active or paused session");
        }

        User updated = createUserInstance(id, name.trim(), trimmedEmail, tier);
        return userRepository.save(updated);
    }

    public User getById(Long id) {
        if (id == null || id <= 0) {
            throw new ValidationException("User id must be a positive integer");
        }
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public List<User> getAll(SubscriptionTier tierFilter) {
        return userRepository.findAll(tierFilter);
    }

    public synchronized void delete(Long id) {
        if (id == null || id <= 0) {
            throw new ValidationException("User id must be a positive integer");
        }
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        if (sessionRepository.hasActiveOrPausedSessionForUser(id)) {
            throw new BusinessRuleException("Cannot delete user with an active or paused session");
        }
        userRepository.deleteById(id);
    }

    private void validateUserData(String name, String email, SubscriptionTier tier) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("User name cannot be empty");
        }
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new ValidationException("User email must be a valid email address");
        }
        if (tier == null) {
            throw new ValidationException("Subscription tier is required");
        }
    }

    private User createUserInstance(Long id, String name, String email, SubscriptionTier tier) {
        if (tier == SubscriptionTier.PREMIUM) {
            return new PremiumUser(id, name, email);
        }
        return new StandardUser(id, name, email);
    }
}
