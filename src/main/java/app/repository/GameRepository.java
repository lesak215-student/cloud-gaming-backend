package app.repository;

import app.model.Game;
import app.model.SubscriptionTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    Optional<Game> findByTitle(String title);

    @Query("SELECT g FROM Game g WHERE " +
           "(:genre IS NULL OR LOWER(g.genre) = LOWER(:genre)) AND " +
           "(:tier IS NULL OR g.requiredTier = :tier)")
    List<Game> findAll(@Param("genre") String genreFilter, @Param("tier") SubscriptionTier tierFilter);
}
