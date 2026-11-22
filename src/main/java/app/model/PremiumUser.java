package app.model;

public class PremiumUser extends User {

    public PremiumUser() {
        super(null, null, null, SubscriptionTier.PREMIUM);
    }

    public PremiumUser(Long id, String name, String email) {
        super(id, name, email, SubscriptionTier.PREMIUM);
    }
}
