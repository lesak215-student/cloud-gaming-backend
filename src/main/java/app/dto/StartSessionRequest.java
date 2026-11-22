package app.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class StartSessionRequest {
    @NotNull(message = "User id is required")
    @Positive(message = "User id must be a positive integer")
    public Long userId;

    @NotNull(message = "Game id is required")
    @Positive(message = "Game id must be a positive integer")
    public Long gameId;

    @Positive(message = "Resolution must be a positive integer")
    public Integer resolution;
}
