package app.repository;

import app.model.GamingSession;
import app.model.SessionStatus;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryGamingSessionRepository implements GamingSessionRepository {

    private final Map<Long, GamingSession> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public GamingSession save(GamingSession session) {
        Long id = session.getId();
        if (id == null) {
            id = idGenerator.incrementAndGet();
            session = new GamingSession(
                    id,
                    session.getUserId(),
                    session.getGameId(),
                    session.getStartedAt(),
                    session.getEndedAt(),
                    session.getResolution(),
                    session.getStatus()
            );
        }
        storage.put(id, session);
        return session;
    }

    @Override
    public Optional<GamingSession> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<GamingSession> findAll(Long userIdFilter, Long gameIdFilter, SessionStatus statusFilter) {
        List<GamingSession> result = new ArrayList<>();
        for (GamingSession session : storage.values()) {
            boolean matchesUser = (userIdFilter == null) || session.getUserId().equals(userIdFilter);
            boolean matchesGame = (gameIdFilter == null) || session.getGameId().equals(gameIdFilter);
            boolean matchesStatus = (statusFilter == null) || session.getStatus() == statusFilter;
            if (matchesUser && matchesGame && matchesStatus) {
                result.add(session);
            }
        }
        return result;
    }

    @Override
    public long countActiveOrPausedSessionsByUserId(Long userId) {
        return storage.values().stream()
                .filter(s -> s.getUserId().equals(userId) && (s.getStatus() == SessionStatus.ACTIVE || s.getStatus() == SessionStatus.PAUSED))
                .count();
    }

    @Override
    public long countActiveOrPausedSessionsByGameId(Long gameId) {
        return storage.values().stream()
                .filter(s -> s.getGameId().equals(gameId) && (s.getStatus() == SessionStatus.ACTIVE || s.getStatus() == SessionStatus.PAUSED))
                .count();
    }

    @Override
    public boolean hasActiveOrPausedSessionForUser(Long userId) {
        return countActiveOrPausedSessionsByUserId(userId) > 0;
    }

    @Override
    public boolean hasActiveOrPausedSessionForGame(Long gameId) {
        return countActiveOrPausedSessionsByGameId(gameId) > 0;
    }

    @Override
    public boolean existsById(Long id) {
        return storage.containsKey(id);
    }

    @Override
    public void deleteById(Long id) {
        storage.remove(id);
    }
}
