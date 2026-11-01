package app.http;

import app.exception.BusinessRuleException;
import app.exception.ResourceNotFoundException;
import app.exception.ValidationException;
import app.model.SessionStatus;
import app.model.SubscriptionTier;
import com.sun.net.httpserver.HttpExchange;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpUtils {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    public static void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] bytes = MAPPER.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    public static void sendNoContent(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    public static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        sendJson(exchange, statusCode, Map.of(
                "status", statusCode,
                "error", resolveStatusText(statusCode),
                "message", message
        ));
    }

    public static <T> T parseBody(HttpExchange exchange, Class<T> clazz) {
        try {
            byte[] bytes = exchange.getRequestBody().readAllBytes();
            if (bytes.length == 0) {
                throw new ValidationException("Request body cannot be empty");
            }
            return MAPPER.readValue(bytes, clazz);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Invalid JSON request body: " + e.getMessage());
        }
    }

    public static void handleException(HttpExchange exchange, Throwable t) throws IOException {
        if (t instanceof ValidationException) {
            sendError(exchange, 400, t.getMessage());
        } else if (t instanceof ResourceNotFoundException) {
            sendError(exchange, 404, t.getMessage());
        } else if (t instanceof BusinessRuleException) {
            sendError(exchange, 409, t.getMessage());
        } else {
            sendError(exchange, 500, "Internal server error: " + t.getMessage());
        }
    }

    public static Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isBlank()) {
            return map;
        }
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            String key = URLDecoder.decode(kv[0].trim(), StandardCharsets.UTF_8);
            String value = kv.length > 1 ? URLDecoder.decode(kv[1].trim(), StandardCharsets.UTF_8) : "";
            map.put(key, value);
        }
        return map;
    }

    public static Long parsePathId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            throw new ValidationException("Identifier in path cannot be empty");
        }
        try {
            long id = Long.parseLong(rawId.trim());
            if (id <= 0) {
                throw new ValidationException("Resource id must be a positive integer, got: " + rawId);
            }
            return id;
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid id format, expected integer, got: " + rawId);
        }
    }

    public static Boolean parseOptionalBoolean(Map<String, String> params, String key) {
        if (!params.containsKey(key)) {
            return null;
        }
        String value = params.get(key).trim();
        if ("true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        throw new ValidationException("Parameter '" + key + "' must be boolean (true/false), got: " + value);
    }

    public static Long parseOptionalLong(Map<String, String> params, String key) {
        if (!params.containsKey(key)) {
            return null;
        }
        String value = params.get(key).trim();
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0) {
                throw new ValidationException("Parameter '" + key + "' must be positive, got: " + value);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new ValidationException("Parameter '" + key + "' must be an integer, got: " + value);
        }
    }

    public static SubscriptionTier parseOptionalTier(Map<String, String> params, String key) {
        if (!params.containsKey(key)) {
            return null;
        }
        String value = params.get(key).trim();
        try {
            return SubscriptionTier.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid subscription tier '" + value + "', expected BASIC or PREMIUM");
        }
    }

    public static SessionStatus parseOptionalStatus(Map<String, String> params, String key) {
        if (!params.containsKey(key)) {
            return null;
        }
        String value = params.get(key).trim();
        try {
            return SessionStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid session status '" + value + "', expected ACTIVE, PAUSED, or COMPLETED");
        }
    }

    private static String resolveStatusText(int statusCode) {
        return switch (statusCode) {
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 409 -> "Conflict";
            default -> "Server Error";
        };
    }
}
