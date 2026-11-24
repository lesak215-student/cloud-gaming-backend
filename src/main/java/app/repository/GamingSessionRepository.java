package app.repository;

import app.model.GamingSession;
import app.model.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GamingSessionRepository extends JpaRepository<GamingSession, Long> {

    @Query("SELECT s FROM GamingSession s JOIN FETCH s.user JOIN FETCH s.game WHERE s.id = :id")
    Optional<GamingSession> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT s FROM GamingSession s JOIN FETCH s.user JOIN FETCH s.game WHERE " +
           "(:userId IS NULL OR s.user.id = :userId) AND " +
           "(:gameId IS NULL OR s.game.id = :gameId) AND " +
           "(:status IS NULL OR s.status = :status)")
    List<GamingSession> findAll(@Param("userId") Long userIdFilter,
                                @Param("gameId") Long gameIdFilter,
                                @Param("status") SessionStatus statusFilter);

    @Query("SELECT s FROM GamingSession s JOIN FETCH s.user JOIN FETCH s.game")
    List<GamingSession> findAllDetailed();

    @Query("SELECT COUNT(s) FROM GamingSession s WHERE s.user.id = :userId AND " +
           "s.status IN (app.model.SessionStatus.ACTIVE, app.model.SessionStatus.PAUSED)")
    long countActiveOrPausedSessionsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(s) FROM GamingSession s WHERE s.game.id = :gameId AND " +
           "s.status IN (app.model.SessionStatus.ACTIVE, app.model.SessionStatus.PAUSED)")
    long countActiveOrPausedSessionsByGameId(@Param("gameId") Long gameId);

    default boolean hasActiveOrPausedSessionForUser(Long userId) {
        return countActiveOrPausedSessionsByUserId(userId) > 0;
    }

    default boolean hasActiveOrPausedSessionForGame(Long gameId) {
        return countActiveOrPausedSessionsByGameId(gameId) > 0;
    }

    @Query("SELECT s FROM GamingSession s JOIN FETCH s.user JOIN FETCH s.game WHERE s.game.id = :gameId")
    List<GamingSession> findByGameId(@Param("gameId") Long gameId);

    @Query("SELECT s FROM GamingSession s JOIN FETCH s.user JOIN FETCH s.game WHERE s.user.id = :userId")
    List<GamingSession> findByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM GamingSession s WHERE s.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM GamingSession s WHERE s.game.id = :gameId")
    void deleteByGameId(@Param("gameId") Long gameId);
}
