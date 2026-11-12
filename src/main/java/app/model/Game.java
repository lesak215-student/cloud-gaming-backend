package app.model;

import java.util.Objects;

public class Game {
    private final Long id;
    private final String title;
    private final String genre;
    private final SubscriptionTier requiredTier;
    private final int maxSlots;

    public Game(Long id, String title, String genre, SubscriptionTier requiredTier, int maxSlots) {
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.requiredTier = requiredTier;
        this.maxSlots = maxSlots;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getGenre() {
        return genre;
    }

    public SubscriptionTier getRequiredTier() {
        return requiredTier;
    }

    public int getMaxSlots() {
        return maxSlots;
    }

    public Game withDetails(String newTitle, String newGenre, SubscriptionTier newTier, int newMaxSlots) {
        return new Game(this.id, newTitle, newGenre, newTier, newMaxSlots);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Game game = (Game) o;
        return Objects.equals(id, game.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
