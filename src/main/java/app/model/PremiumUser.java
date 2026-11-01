package app.model;

public class PremiumUser extends User {

    public PremiumUser(Long id, String name, String email) {
        super(id, name, email);
    }

    @Override
    public SubscriptionTier getSubscriptionTier() {
        return SubscriptionTier.PREMIUM;
    }

    @Override
    public int getMaxConcurrentSessions() {
        return 3;
    }

    @Override
    public int getMaxSessionDurationMinutes() {
        return 360;
    }

    @Override
    public int getMaxStreamingResolution() {
        return 2160;
    }

    @Override
    public boolean canPlayGame(Game game) {
        return true;
    }

    @Override
    public User withDetails(String newName, String newEmail) {
        return new PremiumUser(getId(), newName, newEmail);
    }
}
