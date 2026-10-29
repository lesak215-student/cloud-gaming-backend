package app.repository;

import app.model.PremiumUser;
import app.model.StandardUser;
import app.model.SubscriptionTier;
import app.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class UserRepository {
    private final Map<Long, User> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    public synchronized User save(User user) {
        Long id = user.getId();
        if (id == null) {
            id = idGenerator.incrementAndGet();
            user = reinstanceWithId(user, id);
        }
        storage.put(id, user);
        return user;
    }

    private User reinstanceWithId(User user, Long id) {
        if (user.getSubscriptionTier() == SubscriptionTier.PREMIUM) {
            return new PremiumUser(id, user.getName(), user.getEmail());
        }
        return new StandardUser(id, user.getName(), user.getEmail());
    }

    public Optional<User> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public Optional<User> findByEmail(String email) {
        return storage.values().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email.trim()))
                .findFirst();
    }

    public List<User> findAll(SubscriptionTier tierFilter) {
        List<User> result = new ArrayList<>();
        for (User user : storage.values()) {
            if (tierFilter == null || user.getSubscriptionTier() == tierFilter) {
                result.add(user);
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
