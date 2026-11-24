package app.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "gaming_sessions")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class GamingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(nullable = false)
    private int resolution;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @Column(name = "notes")
    private String notes;

    public GamingSession() {}

    public GamingSession(Long id, User user, Game game, Instant startedAt, Instant endedAt, int resolution, SessionStatus status) {
        this(id, user, game, startedAt, endedAt, resolution, status, null);
    }

    public GamingSession(Long id, User user, Game game, Instant startedAt, Instant endedAt, int resolution, SessionStatus status, String notes) {
        this.id = id;
        this.user = user;
        this.game = game;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.resolution = resolution;
        this.status = status;
        this.notes = notes;
    }

    public GamingSession(Long id, Long userId, Long gameId, Instant startedAt, Instant endedAt, int resolution, SessionStatus status) {
        this(id, userId, gameId, startedAt, endedAt, resolution, status, null);
    }

    public GamingSession(Long id, Long userId, Long gameId, Instant startedAt, Instant endedAt, int resolution, SessionStatus status, String notes) {
        this.id = id;
        this.user = userId != null ? new User(userId, null, null, null) : null;
        this.game = gameId != null ? new Game(gameId, null, null, null, 0) : null;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.resolution = resolution;
        this.status = status;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    public Long getGameId() {
        return game != null ? game.getId() : null;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public int getResolution() {
        return resolution;
    }

    public void setResolution(int resolution) {
        this.resolution = resolution;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isActive() {
        return status == SessionStatus.ACTIVE;
    }

    public boolean isPaused() {
        return status == SessionStatus.PAUSED;
    }

    public boolean isCompleted() {
        return status == SessionStatus.COMPLETED;
    }

    public GamingSession paused() {
        return new GamingSession(id, user, game, startedAt, endedAt, resolution, SessionStatus.PAUSED, notes);
    }

    public GamingSession resumed() {
        return new GamingSession(id, user, game, startedAt, endedAt, resolution, SessionStatus.ACTIVE, notes);
    }

    public GamingSession completed(Instant endedAt) {
        return new GamingSession(id, user, game, startedAt, endedAt, resolution, SessionStatus.COMPLETED, notes);
    }

    public GamingSession completed(Instant endedAt, String notes) {
        return new GamingSession(id, user, game, startedAt, endedAt, resolution, SessionStatus.COMPLETED, notes);
    }

    public GamingSession withResolution(int newResolution) {
        return new GamingSession(id, user, game, startedAt, endedAt, newResolution, status, notes);
    }

    public GamingSession withNotes(String notes) {
        return new GamingSession(id, user, game, startedAt, endedAt, resolution, status, notes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GamingSession that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
