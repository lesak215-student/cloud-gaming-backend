package app.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class UpdateSessionRequest {
    @NotNull(message = "Resolution is required")
    @Positive(message = "Resolution must be a positive integer")
    public Integer resolution;
}
