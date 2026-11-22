package app.repository;

import app.model.GamingSession;
import app.model.SessionStatus;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryGamingSessionRepository {

    public static GamingSessionRepository create() {
        Map<Long, GamingSession> storage = new ConcurrentHashMap<>();
        AtomicLong idGenerator = new AtomicLong(0);

        return (GamingSessionRepository) Proxy.newProxyInstance(
                GamingSessionRepository.class.getClassLoader(),
                new Class<?>[]{GamingSessionRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "save" -> {
                        GamingSession session = (GamingSession) args[0];
                        if (session.getId() == null) {
                            session.setId(idGenerator.incrementAndGet());
                        }
                        storage.put(session.getId(), session);
                        yield session;
                    }
                    case "findById" -> {
                        Long id = (Long) args[0];
                        yield Optional.ofNullable(storage.get(id));
                    }
                    case "findAll" -> {
                        if (args != null && args.length == 3) {
                            Long userIdFilter = (Long) args[0];
                            Long gameIdFilter = (Long) args[1];
                            SessionStatus statusFilter = (SessionStatus) args[2];
                            List<GamingSession> result = new ArrayList<>();
                            for (GamingSession session : storage.values()) {
                                boolean matchesUser = (userIdFilter == null) || (session.getUserId() != null && session.getUserId().equals(userIdFilter));
                                boolean matchesGame = (gameIdFilter == null) || (session.getGameId() != null && session.getGameId().equals(gameIdFilter));
                                boolean matchesStatus = (statusFilter == null) || session.getStatus() == statusFilter;
                                if (matchesUser && matchesGame && matchesStatus) {
                                    result.add(session);
                                }
                            }
                            yield result;
                        }
                        yield new ArrayList<>(storage.values());
                    }
                    case "findAllWithUserAndGame", "findAllDetailed" -> new ArrayList<>(storage.values());
                    case "countActiveOrPausedSessionsByUserId" -> {
                        Long userId = (Long) args[0];
                        yield storage.values().stream()
                                .filter(s -> s.getUserId() != null && s.getUserId().equals(userId)
                                        && (s.getStatus() == SessionStatus.ACTIVE || s.getStatus() == SessionStatus.PAUSED))
                                .count();
                    }
                    case "countActiveOrPausedSessionsByGameId" -> {
                        Long gameId = (Long) args[0];
                        yield storage.values().stream()
                                .filter(s -> s.getGameId() != null && s.getGameId().equals(gameId)
                                        && (s.getStatus() == SessionStatus.ACTIVE || s.getStatus() == SessionStatus.PAUSED))
                                .count();
                    }
                    case "hasActiveOrPausedSessionForUser" -> {
                        Long userId = (Long) args[0];
                        yield storage.values().stream()
                                .anyMatch(s -> s.getUserId() != null && s.getUserId().equals(userId)
                                        && (s.getStatus() == SessionStatus.ACTIVE || s.getStatus() == SessionStatus.PAUSED));
                    }
                    case "hasActiveOrPausedSessionForGame" -> {
                        Long gameId = (Long) args[0];
                        yield storage.values().stream()
                                .anyMatch(s -> s.getGameId() != null && s.getGameId().equals(gameId)
                                        && (s.getStatus() == SessionStatus.ACTIVE || s.getStatus() == SessionStatus.PAUSED));
                    }
                    case "findByGameId" -> {
                        Long gameId = (Long) args[0];
                        yield storage.values().stream()
                                .filter(s -> s.getGameId() != null && s.getGameId().equals(gameId))
                                .toList();
                    }
                    case "findByUserId" -> {
                        Long userId = (Long) args[0];
                        yield storage.values().stream()
                                .filter(s -> s.getUserId() != null && s.getUserId().equals(userId))
                                .toList();
                    }
                    case "deleteByUserId" -> {
                        Long userId = (Long) args[0];
                        storage.values().removeIf(s -> s.getUserId() != null && s.getUserId().equals(userId));
                        yield null;
                    }
                    case "deleteByGameId" -> {
                        Long gameId = (Long) args[0];
                        storage.values().removeIf(s -> s.getGameId() != null && s.getGameId().equals(gameId));
                        yield null;
                    }
                    case "existsById" -> storage.containsKey((Long) args[0]);
                    case "deleteById" -> {
                        storage.remove((Long) args[0]);
                        yield null;
                    }
                    case "delete" -> {
                        GamingSession s = (GamingSession) args[0];
                        if (s != null && s.getId() != null) {
                            storage.remove(s.getId());
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
                    case "toString" -> "InMemoryGamingSessionRepository@" + Integer.toHexString(System.identityHashCode(proxy));
                    default -> throw new UnsupportedOperationException("Method " + method.getName() + " is not implemented in InMemoryGamingSessionRepository");
                }
        );
    }
}
