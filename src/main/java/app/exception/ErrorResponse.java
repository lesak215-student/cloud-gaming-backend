package app.exception;

public record ErrorResponse(String error, String message) {
    public ErrorResponse(String message) {
        this(message, message);
    }
}
