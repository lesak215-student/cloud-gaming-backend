package app.service;

import app.exception.BusinessRuleException;
import app.exception.ResourceNotFoundException;
import app.exception.ValidationException;
import app.model.Game;
import app.model.GamingSession;
import app.model.SessionStatus;
import app.model.User;
import app.repository.GameRepository;
import app.repository.GamingSessionRepository;
import app.repository.UserRepository;

import java.time.Instant;
import java.util.List;

public class GamingSessionService {
    private final GamingSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;

    public GamingSessionService(GamingSessionRepository sessionRepository, UserRepository userRepository, GameRepository gameRepository) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
    }

    public synchronized GamingSession start(Long userId, Long gameId, Integer resolution) {
        if (userId == null || userId <= 0) {
            throw new ValidationException("User id must be a positive integer");
        }
        if (gameId == null || gameId <= 0) {
            throw new ValidationException("Game id must be a positive integer");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found with id: " + gameId));

        int effectiveResolution = (resolution == null || resolution <= 0) ? user.getMaxStreamingResolution() : resolution;
        if (effectiveResolution > user.getMaxStreamingResolution()) {
            throw new BusinessRuleException("Resolution " + effectiveResolution + "p exceeds maximum allowed for user tier: " + user.getMaxStreamingResolution() + "p");
        }

        if (!user.canPlayGame(game)) {
            throw new BusinessRuleException("User subscription tier " + user.getSubscriptionTier() + " does not allow playing game with required tier " + game.getRequiredTier());
        }

        long activeUserSessions = sessionRepository.countActiveOrPausedSessionsByUserId(userId);
        if (activeUserSessions >= user.getMaxConcurrentSessions()) {
            throw new BusinessRuleException("User has reached maximum concurrent sessions limit: " + user.getMaxConcurrentSessions());
        }

        long activeGameSessions = sessionRepository.countActiveOrPausedSessionsByGameId(gameId);
        if (activeGameSessions >= game.getMaxSlots()) {
            throw new BusinessRuleException("All " + game.getMaxSlots() + " slots are occupied for game: " + game.getTitle());
        }

        GamingSession session = new GamingSession(null, userId, gameId, Instant.now(), null, effectiveResolution, SessionStatus.ACTIVE);
        return sessionRepository.save(session);
    }

    public synchronized GamingSession pause(Long id) {
        GamingSession session = getById(id);
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new BusinessRuleException("Cannot pause session with status: " + session.getStatus());
        }
        GamingSession updated = session.paused();
        return sessionRepository.save(updated);
    }

    public synchronized GamingSession resume(Long id) {
        GamingSession session = getById(id);
        if (session.getStatus() != SessionStatus.PAUSED) {
            throw new BusinessRuleException("Cannot resume session with status: " + session.getStatus());
        }
        GamingSession updated = session.resumed();
        return sessionRepository.save(updated);
    }

    public synchronized GamingSession terminate(Long id) {
        GamingSession session = getById(id);
        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new BusinessRuleException("Session is already completed");
        }
        GamingSession updated = session.completed(Instant.now());
        return sessionRepository.save(updated);
    }

    public synchronized GamingSession update(Long id, Integer newResolution) {
        if (newResolution == null || newResolution <= 0) {
            throw new ValidationException("New resolution must be a positive integer");
        }
        GamingSession session = getById(id);
        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new BusinessRuleException("Cannot modify resolution of completed session");
        }
        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + session.getUserId()));
        if (newResolution > user.getMaxStreamingResolution()) {
            throw new BusinessRuleException("Resolution " + newResolution + "p exceeds maximum allowed for user tier: " + user.getMaxStreamingResolution() + "p");
        }
        GamingSession updated = session.withResolution(newResolution);
        return sessionRepository.save(updated);
    }

    public GamingSession getById(Long id) {
        if (id == null || id <= 0) {
            throw new ValidationException("Session id must be a positive integer");
        }
        return sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + id));
    }

    public List<GamingSession> getAll(Long userIdFilter, Long gameIdFilter, SessionStatus statusFilter) {
        return sessionRepository.findAll(userIdFilter, gameIdFilter, statusFilter);
    }

    public synchronized void delete(Long id) {
        GamingSession session = getById(id);
        if (session.getStatus() != SessionStatus.COMPLETED) {
            throw new BusinessRuleException("Cannot delete session that is not completed; terminate it first");
        }
        sessionRepository.deleteById(id);
    }
}
