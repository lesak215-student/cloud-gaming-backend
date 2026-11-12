package app.repository;

import app.model.GamingSession;
import app.model.SessionStatus;

import java.util.List;
import java.util.Optional;

public interface GamingSessionRepository {

    GamingSession save(GamingSession session);

    Optional<GamingSession> findById(Long id);

    List<GamingSession> findAll(Long userIdFilter, Long gameIdFilter, SessionStatus statusFilter);

    long countActiveOrPausedSessionsByUserId(Long userId);

    long countActiveOrPausedSessionsByGameId(Long gameId);

    boolean hasActiveOrPausedSessionForUser(Long userId);

    boolean hasActiveOrPausedSessionForGame(Long gameId);

    boolean existsById(Long id);

    void deleteById(Long id);
}
