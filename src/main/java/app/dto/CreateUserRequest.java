package app.dto;

import app.model.SubscriptionTier;

public class CreateUserRequest {
    public String name;
    public String email;
    public SubscriptionTier subscriptionTier;
}
