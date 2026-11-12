package app.repository;

import app.model.Game;
import app.model.SubscriptionTier;

import java.util.List;
import java.util.Optional;

public interface GameRepository {

    Game save(Game game);

    Optional<Game> findById(Long id);

    Optional<Game> findByTitle(String title);

    List<Game> findAll(String genreFilter, SubscriptionTier tierFilter);

    boolean existsById(Long id);

    void deleteById(Long id);
}
