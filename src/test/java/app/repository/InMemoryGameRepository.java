package app.repository;

import app.model.Game;
import app.model.SubscriptionTier;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryGameRepository {

    public static GameRepository create() {
        return create(null);
    }

    public static GameRepository create(GamingSessionRepository sessionRepository) {
        Map<Long, Game> storage = new ConcurrentHashMap<>();
        AtomicLong idGenerator = new AtomicLong(0);

        return (GameRepository) Proxy.newProxyInstance(
                GameRepository.class.getClassLoader(),
                new Class<?>[]{GameRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "save" -> {
                        Game game = (Game) args[0];
                        if (game.getId() == null) {
                            game.setId(idGenerator.incrementAndGet());
                        }
                        storage.put(game.getId(), game);
                        yield game;
                    }
                    case "findById", "findByIdForUpdate" -> {
                        Long id = (Long) args[0];
                        yield Optional.ofNullable(storage.get(id));
                    }
                    case "findByTitle" -> {
                        String title = (String) args[0];
                        yield storage.values().stream()
                                .filter(g -> g.getTitle() != null && g.getTitle().equalsIgnoreCase(title.trim()))
                                .findFirst();
                    }
                    case "findAll" -> {
                        if (args != null && args.length == 2) {
                            String genreFilter = (String) args[0];
                            SubscriptionTier tierFilter = (SubscriptionTier) args[1];
                            List<Game> result = new ArrayList<>();
                            for (Game g : storage.values()) {
                                boolean matchesGenre = (genreFilter == null) || g.getGenre().equalsIgnoreCase(genreFilter.trim());
                                boolean matchesTier = (tierFilter == null) || g.getRequiredTier() == tierFilter;
                                if (matchesGenre && matchesTier) {
                                    result.add(g);
                                }
                            }
                            yield result;
                        }
                        yield new ArrayList<>(storage.values());
                    }
                    case "findAllWithFilters" -> {
                        String genreFilter = (String) args[0];
                        SubscriptionTier tierFilter = (SubscriptionTier) args[1];
                        Boolean hasSlotsFilter = (Boolean) args[2];
                        List<Game> result = new ArrayList<>();
                        for (Game g : storage.values()) {
                            boolean matchesGenre = (genreFilter == null) || g.getGenre().equalsIgnoreCase(genreFilter.trim());
                            boolean matchesTier = (tierFilter == null) || g.getRequiredTier() == tierFilter;
                            boolean matchesSlots = true;
                            if (hasSlotsFilter != null && sessionRepository != null) {
                                long activeCount = sessionRepository.countActiveOrPausedSessionsByGameId(g.getId());
                                boolean hasSlots = activeCount < g.getMaxSlots();
                                matchesSlots = (hasSlotsFilter.booleanValue() == hasSlots);
                            }
                            if (matchesGenre && matchesTier && matchesSlots) {
                                result.add(g);
                            }
                        }
                        yield result;
                    }
                    case "existsById" -> storage.containsKey((Long) args[0]);
                    case "deleteById" -> {
                        storage.remove((Long) args[0]);
                        yield null;
                    }
                    case "delete" -> {
                        Game g = (Game) args[0];
                        if (g != null && g.getId() != null) {
                            storage.remove(g.getId());
                        }
                        yield null;
                    }
                    case "deleteAll" -> {
                        storage.clear();
                        yield null;
                    }
                    case "count" -> (long) storage.size();
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "InMemoryGameRepository@" + Integer.toHexString(System.identityHashCode(proxy));
                    default -> throw new UnsupportedOperationException("Method " + method.getName() + " is not implemented in InMemoryGameRepository");
                }
        );
    }
}
