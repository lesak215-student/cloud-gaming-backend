package app.repository;

import app.model.SubscriptionTier;
import app.model.User;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryUserRepository {

    public static UserRepository create() {
        Map<Long, User> storage = new ConcurrentHashMap<>();
        AtomicLong idGenerator = new AtomicLong(0);

        return (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "save" -> {
                        User user = (User) args[0];
                        if (user.getId() == null) {
                            user.setId(idGenerator.incrementAndGet());
                        }
                        storage.put(user.getId(), user);
                        yield user;
                    }
                    case "findById" -> {
                        Long id = (Long) args[0];
                        yield Optional.ofNullable(storage.get(id));
                    }
                    case "findByEmail" -> {
                        String email = (String) args[0];
                        yield storage.values().stream()
                                .filter(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(email.trim()))
                                .findFirst();
                    }
                    case "findAllBySubscriptionTier" -> {
                        SubscriptionTier tier = (SubscriptionTier) args[0];
                        yield storage.values().stream()
                                .filter(u -> u.getSubscriptionTier() == tier)
                                .toList();
                    }
                    case "findAll" -> {
                        if (args != null && args.length > 0 && args[0] instanceof SubscriptionTier tier) {
                            yield storage.values().stream()
                                    .filter(u -> u.getSubscriptionTier() == tier)
                                    .toList();
                        }
                        yield new ArrayList<>(storage.values());
                    }
                    case "existsById" -> storage.containsKey((Long) args[0]);
                    case "deleteById" -> {
                        storage.remove((Long) args[0]);
                        yield null;
                    }
                    case "delete" -> {
                        User u = (User) args[0];
                        if (u != null && u.getId() != null) {
                            storage.remove(u.getId());
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
                    case "toString" -> "InMemoryUserRepository@" + Integer.toHexString(System.identityHashCode(proxy));
                    default -> throw new UnsupportedOperationException("Method " + method.getName() + " is not implemented in InMemoryUserRepository");
                }
        );
    }
}
