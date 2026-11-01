package app.dto;

import app.model.SubscriptionTier;

public class UpdateUserRequest {
    public String name;
    public String email;
    public SubscriptionTier subscriptionTier;
}
