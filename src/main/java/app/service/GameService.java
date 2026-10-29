package app.service;

import app.exception.BusinessRuleException;
import app.exception.ResourceNotFoundException;
import app.exception.ValidationException;
import app.model.Game;
import app.model.SubscriptionTier;
import app.repository.GameRepository;
import app.repository.GamingSessionRepository;

import java.util.List;

public class GameService {
    private final GameRepository gameRepository;
    private final GamingSessionRepository sessionRepository;

    public GameService(GameRepository gameRepository, GamingSessionRepository sessionRepository) {
        this.gameRepository = gameRepository;
        this.sessionRepository = sessionRepository;
    }

    public synchronized Game create(String title, String genre, SubscriptionTier requiredTier, int maxSlots) {
        validateGameData(title, genre, requiredTier, maxSlots);
        String trimmedTitle = title.trim();
        if (gameRepository.findByTitle(trimmedTitle).isPresent()) {
            throw new BusinessRuleException("Game with title already exists: " + trimmedTitle);
        }
        Game game = new Game(null, trimmedTitle, genre.trim(), requiredTier, maxSlots);
        return gameRepository.save(game);
    }

    public synchronized Game update(Long id, String title, String genre, SubscriptionTier requiredTier, int maxSlots) {
        if (id == null || id <= 0) {
            throw new ValidationException("Game id must be a positive integer");
        }
        Game existing = getById(id);
        validateGameData(title, genre, requiredTier, maxSlots);

        String trimmedTitle = title.trim();
        gameRepository.findByTitle(trimmedTitle).ifPresent(gameWithSameTitle -> {
            if (!gameWithSameTitle.getId().equals(id)) {
                throw new BusinessRuleException("Game title is already taken: " + trimmedTitle);
            }
        });

        long activeSessions = sessionRepository.countActiveOrPausedSessionsByGameId(id);
        if (maxSlots < activeSessions) {
            throw new BusinessRuleException("Cannot decrease max slots below current active sessions count: " + activeSessions);
        }

        Game updated = existing.withDetails(trimmedTitle, genre.trim(), requiredTier, maxSlots);
        return gameRepository.save(updated);
    }

    public Game getById(Long id) {
        if (id == null || id <= 0) {
            throw new ValidationException("Game id must be a positive integer");
        }
        return gameRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found with id: " + id));
    }

    public List<Game> getAll(String genreFilter, SubscriptionTier tierFilter, Boolean hasSlotsFilter) {
        List<Game> games = gameRepository.findAll(genreFilter, tierFilter);
        if (hasSlotsFilter == null) {
            return games;
        }
        return games.stream()
                .filter(game -> {
                    long activeCount = sessionRepository.countActiveOrPausedSessionsByGameId(game.getId());
                    boolean hasAvailableSlots = activeCount < game.getMaxSlots();
                    return hasSlotsFilter.booleanValue() == hasAvailableSlots;
                })
                .toList();
    }

    public synchronized void delete(Long id) {
        if (id == null || id <= 0) {
            throw new ValidationException("Game id must be a positive integer");
        }
        if (!gameRepository.existsById(id)) {
            throw new ResourceNotFoundException("Game not found with id: " + id);
        }
        if (sessionRepository.hasActiveOrPausedSessionForGame(id)) {
            throw new BusinessRuleException("Cannot delete game that has active or paused sessions");
        }
        gameRepository.deleteById(id);
    }

    private void validateGameData(String title, String genre, SubscriptionTier requiredTier, int maxSlots) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Game title cannot be empty");
        }
        if (genre == null || genre.isBlank()) {
            throw new ValidationException("Game genre cannot be empty");
        }
        if (requiredTier == null) {
            throw new ValidationException("Required tier must be specified");
        }
        if (maxSlots <= 0) {
            throw new ValidationException("Max slots must be greater than zero");
        }
    }
}
