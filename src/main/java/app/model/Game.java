package app.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "games")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String title;

    @Column(nullable = false)
    private String genre;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_tier", nullable = false)
    private SubscriptionTier requiredTier;

    @Column(name = "max_slots", nullable = false)
    private int maxSlots;

    public Game() {}

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

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public SubscriptionTier getRequiredTier() {
        return requiredTier;
    }

    public void setRequiredTier(SubscriptionTier requiredTier) {
        this.requiredTier = requiredTier;
    }

    public int getMaxSlots() {
        return maxSlots;
    }

    public void setMaxSlots(int maxSlots) {
        this.maxSlots = maxSlots;
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
