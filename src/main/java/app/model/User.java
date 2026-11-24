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
@Table(name = "users")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_tier", nullable = false)
    private SubscriptionTier subscriptionTier;

    public User() {}

    public User(Long id, String name, String email, SubscriptionTier subscriptionTier) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.subscriptionTier = subscriptionTier;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public SubscriptionTier getSubscriptionTier() {
        return subscriptionTier;
    }

    public void setSubscriptionTier(SubscriptionTier subscriptionTier) {
        this.subscriptionTier = subscriptionTier;
    }

    public int getMaxConcurrentSessions() {
        return subscriptionTier == SubscriptionTier.PREMIUM ? 3 : 1;
    }

    public int getMaxStreamingResolution() {
        return subscriptionTier == SubscriptionTier.PREMIUM ? 2160 : 1080;
    }

    public boolean canPlayGame(Game game) {
        if (subscriptionTier == SubscriptionTier.PREMIUM) {
            return true;
        }
        return game != null && game.getRequiredTier() == SubscriptionTier.BASIC;
    }

    public User withDetails(String newName, String newEmail) {
        return new User(this.id, newName, newEmail, this.subscriptionTier);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
