package app.repository;

import app.model.Game;
import app.model.SubscriptionTier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class GameRepository {
    private final Map<Long, Game> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    public synchronized Game save(Game game) {
        Long id = game.getId();
        if (id == null) {
            id = idGenerator.incrementAndGet();
            game = new Game(id, game.getTitle(), game.getGenre(), game.getRequiredTier(), game.getMaxSlots());
        }
        storage.put(id, game);
        return game;
    }

    public Optional<Game> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public Optional<Game> findByTitle(String title) {
        return storage.values().stream()
                .filter(g -> g.getTitle().equalsIgnoreCase(title.trim()))
                .findFirst();
    }

    public List<Game> findAll(String genreFilter, SubscriptionTier tierFilter) {
        List<Game> result = new ArrayList<>();
        for (Game game : storage.values()) {
            boolean matchesGenre = (genreFilter == null || genreFilter.isBlank()) || game.getGenre().equalsIgnoreCase(genreFilter.trim());
            boolean matchesTier = (tierFilter == null) || game.getRequiredTier() == tierFilter;
            if (matchesGenre && matchesTier) {
                result.add(game);
            }
        }
        return result;
    }

    public boolean existsById(Long id) {
        return storage.containsKey(id);
    }

    public synchronized void deleteById(Long id) {
        storage.remove(id);
    }
}
