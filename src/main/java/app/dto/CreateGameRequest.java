package app.dto;

import app.model.SubscriptionTier;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateGameRequest {
    @NotBlank(message = "Game title cannot be empty")
    public String title;

    @NotBlank(message = "Game genre cannot be empty")
    public String genre;

    @NotNull(message = "Required tier must be specified")
    public SubscriptionTier requiredTier;

    @NotNull(message = "Max slots must be specified")
    @Min(value = 1, message = "Max slots must be greater than zero")
    public Integer maxSlots;
}
