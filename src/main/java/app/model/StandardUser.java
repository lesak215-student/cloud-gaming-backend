package app.model;

public class StandardUser extends User {

    public StandardUser() {
        super(null, null, null, SubscriptionTier.BASIC);
    }

    public StandardUser(Long id, String name, String email) {
        super(id, name, email, SubscriptionTier.BASIC);
    }
}
