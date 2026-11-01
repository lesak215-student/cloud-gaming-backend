package app.model;

import java.time.Instant;
import java.util.Objects;

public class GamingSession {
    private final Long id;
    private final Long userId;
    private final Long gameId;
    private final Instant startedAt;
    private final Instant endedAt;
    private final int resolution;
    private final SessionStatus status;

    public GamingSession(Long id, Long userId, Long gameId, Instant startedAt, Instant endedAt, int resolution, SessionStatus status) {
        this.id = id;
        this.userId = userId;
        this.gameId = gameId;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.resolution = resolution;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getGameId() {
        return gameId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public int getResolution() {
        return resolution;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public GamingSession paused() {
        return new GamingSession(id, userId, gameId, startedAt, endedAt, resolution, SessionStatus.PAUSED);
    }

    public GamingSession resumed() {
        return new GamingSession(id, userId, gameId, startedAt, endedAt, resolution, SessionStatus.ACTIVE);
    }

    public GamingSession completed(Instant endedAt) {
        return new GamingSession(id, userId, gameId, startedAt, endedAt, resolution, SessionStatus.COMPLETED);
    }

    public GamingSession withResolution(int newResolution) {
        return new GamingSession(id, userId, gameId, startedAt, endedAt, newResolution, status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GamingSession that = (GamingSession) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
