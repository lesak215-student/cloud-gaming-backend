package app.repository;

import app.model.Game;
import app.model.SubscriptionTier;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    Optional<Game> findByTitle(String title);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM Game g WHERE g.id = :id")
    Optional<Game> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT g FROM Game g WHERE " +
           "(:genre IS NULL OR LOWER(g.genre) = LOWER(:genre)) AND " +
           "(:tier IS NULL OR g.requiredTier = :tier)")
    List<Game> findAll(@Param("genre") String genreFilter, @Param("tier") SubscriptionTier tierFilter);

    @Query("SELECT g FROM Game g WHERE " +
           "(:genre IS NULL OR LOWER(g.genre) = LOWER(:genre)) AND " +
           "(:tier IS NULL OR g.requiredTier = :tier) AND " +
           "(:hasSlots IS NULL OR " +
           "  (:hasSlots = true AND (SELECT COUNT(s) FROM GamingSession s WHERE s.game = g AND s.status IN (app.model.SessionStatus.ACTIVE, app.model.SessionStatus.PAUSED)) < g.maxSlots) OR " +
           "  (:hasSlots = false AND (SELECT COUNT(s) FROM GamingSession s WHERE s.game = g AND s.status IN (app.model.SessionStatus.ACTIVE, app.model.SessionStatus.PAUSED)) >= g.maxSlots)" +
           ")")
    List<Game> findAllWithFilters(@Param("genre") String genreFilter,
                                 @Param("tier") SubscriptionTier tierFilter,
                                 @Param("hasSlots") Boolean hasSlotsFilter);
}
