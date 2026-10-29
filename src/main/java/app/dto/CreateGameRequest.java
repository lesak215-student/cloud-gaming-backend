package app.dto;

import app.model.SubscriptionTier;

public class CreateGameRequest {
    public String title;
    public String genre;
    public SubscriptionTier requiredTier;
    public Integer maxSlots;
}
