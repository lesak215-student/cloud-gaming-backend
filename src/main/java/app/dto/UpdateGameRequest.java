package app.dto;

import app.model.SubscriptionTier;

public class UpdateGameRequest {
    public String title;
    public String genre;
    public SubscriptionTier requiredTier;
    public Integer maxSlots;
}
