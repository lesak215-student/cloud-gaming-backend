package app.repository;

import app.model.GamingSession;
import app.model.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GamingSessionRepository extends JpaRepository<GamingSession, Long> {

    @Query("SELECT s FROM GamingSession s WHERE " +
           "(:userId IS NULL OR s.user.id = :userId) AND " +
           "(:gameId IS NULL OR s.game.id = :gameId) AND " +
           "(:status IS NULL OR s.status = :status)")
    List<GamingSession> findAll(@Param("userId") Long userIdFilter,
                                @Param("gameId") Long gameIdFilter,
                                @Param("status") SessionStatus statusFilter);

    @Query("SELECT COUNT(s) FROM GamingSession s WHERE s.user.id = :userId AND (s.status = 'ACTIVE' OR s.status = 'PAUSED')")
    long countActiveOrPausedSessionsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(s) FROM GamingSession s WHERE s.game.id = :gameId AND (s.status = 'ACTIVE' OR s.status = 'PAUSED')")
    long countActiveOrPausedSessionsByGameId(@Param("gameId") Long gameId);

    default boolean hasActiveOrPausedSessionForUser(Long userId) {
        return countActiveOrPausedSessionsByUserId(userId) > 0;
    }

    default boolean hasActiveOrPausedSessionForGame(Long gameId) {
        return countActiveOrPausedSessionsByGameId(gameId) > 0;
    }

    List<GamingSession> findByGameId(Long gameId);

    List<GamingSession> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    void deleteByGameId(Long gameId);
}
