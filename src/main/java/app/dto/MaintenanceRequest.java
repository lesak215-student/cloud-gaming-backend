package app.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class MaintenanceRequest {

    @NotNull(message = "New max slots must be specified")
    @Min(value = 0, message = "New max slots cannot be negative")
    public Integer newMaxSlots;

    @NotBlank(message = "Maintenance reason cannot be empty")
    public String reason;

    public MaintenanceRequest() {}

    public MaintenanceRequest(Integer newMaxSlots, String reason) {
        this.newMaxSlots = newMaxSlots;
        this.reason = reason;
    }
}
