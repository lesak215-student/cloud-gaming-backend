package app.dto;

import jakarta.validation.constraints.Positive;

public class UpdateSessionRequest {
    @Positive(message = "Resolution must be a positive integer")
    public Integer resolution;

    public String notes;
}
