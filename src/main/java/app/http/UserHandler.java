package app.http;

import app.dto.CreateUserRequest;
import app.dto.UpdateUserRequest;
import app.model.SubscriptionTier;
import app.model.User;
import app.service.UserService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class UserHandler implements HttpHandler {

    private final UserService userService;

    public UserHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            String subPath = extractSubPath(path, "/users");

            if (subPath.isEmpty()) {
                handleCollection(exchange, method);
            } else {
                handleItem(exchange, method, subPath);
            }
        } catch (Throwable t) {
            HttpUtils.handleException(exchange, t);
        }
    }

    private void handleCollection(HttpExchange exchange, String method) throws IOException {
        switch (method) {
            case "GET" -> {
                Map<String, String> params = HttpUtils.parseQueryParams(exchange.getRequestURI().getQuery());
                SubscriptionTier tierFilter = HttpUtils.parseOptionalTier(params, "tier");
                List<User> users = userService.getAll(tierFilter);
                HttpUtils.sendJson(exchange, 200, users);
            }
            case "POST" -> {
                CreateUserRequest request = HttpUtils.parseBody(exchange, CreateUserRequest.class);
                User user = userService.create(request.name, request.email, request.subscriptionTier);
                HttpUtils.sendJson(exchange, 201, user);
            }
            default -> {
                exchange.getResponseHeaders().set("Allow", "GET, POST");
                HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            }
        }
    }

    private void handleItem(HttpExchange exchange, String method, String subPath) throws IOException {
        String[] parts = subPath.split("/");
        if (parts.length != 1) {
            HttpUtils.sendError(exchange, 404, "Unknown path: " + exchange.getRequestURI().getPath());
            return;
        }

        Long id = HttpUtils.parsePathId(parts[0]);

        switch (method) {
            case "GET" -> {
                User user = userService.getById(id);
                HttpUtils.sendJson(exchange, 200, user);
            }
            case "PUT" -> {
                UpdateUserRequest request = HttpUtils.parseBody(exchange, UpdateUserRequest.class);
                User updated = userService.update(id, request.name, request.email, request.subscriptionTier);
                HttpUtils.sendJson(exchange, 200, updated);
            }
            case "DELETE" -> {
                userService.delete(id);
                HttpUtils.sendNoContent(exchange);
            }
            default -> {
                exchange.getResponseHeaders().set("Allow", "GET, PUT, DELETE");
                HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            }
        }
    }

    private String extractSubPath(String path, String prefix) {
        if (!path.startsWith(prefix)) {
            return "";
        }
        String sub = path.substring(prefix.length());
        if (sub.startsWith("/")) {
            sub = sub.substring(1);
        }
        if (sub.endsWith("/")) {
            sub = sub.substring(0, sub.length() - 1);
        }
        return sub;
    }
}
