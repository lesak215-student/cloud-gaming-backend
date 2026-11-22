package app.dto;

import app.model.SubscriptionTier;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateUserRequest {
    @NotBlank(message = "User name cannot be empty")
    public String name;

    @NotBlank(message = "User email cannot be empty")
    @Email(message = "User email must be a valid email address")
    public String email;

    @NotNull(message = "Subscription tier is required")
    public SubscriptionTier subscriptionTier;
}
