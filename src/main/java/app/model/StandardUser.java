package app.model;

public class StandardUser extends User {

    public StandardUser(Long id, String name, String email) {
        super(id, name, email);
    }

    @Override
    public SubscriptionTier getSubscriptionTier() {
        return SubscriptionTier.BASIC;
    }

    @Override
    public int getMaxConcurrentSessions() {
        return 1;
    }

    @Override
    public int getMaxSessionDurationMinutes() {
        return 60;
    }

    @Override
    public int getMaxStreamingResolution() {
        return 1080;
    }

    @Override
    public boolean canPlayGame(Game game) {
        return game.getRequiredTier() == SubscriptionTier.BASIC;
    }

    @Override
    public User withDetails(String newName, String newEmail) {
        return new StandardUser(getId(), newName, newEmail);
    }
}
