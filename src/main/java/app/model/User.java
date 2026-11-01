package app.model;

import java.util.Objects;

public abstract class User {
    private final Long id;
    private final String name;
    private final String email;

    protected User(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public abstract SubscriptionTier getSubscriptionTier();

    public abstract int getMaxConcurrentSessions();

    public abstract int getMaxSessionDurationMinutes();

    public abstract int getMaxStreamingResolution();

    public abstract boolean canPlayGame(Game game);

    public abstract User withDetails(String newName, String newEmail);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
