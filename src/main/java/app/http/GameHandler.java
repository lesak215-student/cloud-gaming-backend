package app.http;

import app.dto.CreateGameRequest;
import app.dto.UpdateGameRequest;
import app.model.Game;
import app.model.SubscriptionTier;
import app.service.GameService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class GameHandler implements HttpHandler {

    private final GameService gameService;

    public GameHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            String subPath = extractSubPath(path, "/games");

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
                String genreFilter = params.get("genre");
                SubscriptionTier tierFilter = HttpUtils.parseOptionalTier(params, "tier");
                Boolean hasSlotsFilter = HttpUtils.parseOptionalBoolean(params, "hasSlots");

                List<Game> games = gameService.getAll(genreFilter, tierFilter, hasSlotsFilter);
                HttpUtils.sendJson(exchange, 200, games);
            }
            case "POST" -> {
                CreateGameRequest request = HttpUtils.parseBody(exchange, CreateGameRequest.class);
                Game game = gameService.create(request.title, request.genre, request.requiredTier, request.maxSlots != null ? request.maxSlots : 0);
                HttpUtils.sendJson(exchange, 201, game);
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
                Game game = gameService.getById(id);
                HttpUtils.sendJson(exchange, 200, game);
            }
            case "PUT" -> {
                UpdateGameRequest request = HttpUtils.parseBody(exchange, UpdateGameRequest.class);
                Game updated = gameService.update(id, request.title, request.genre, request.requiredTier, request.maxSlots != null ? request.maxSlots : 0);
                HttpUtils.sendJson(exchange, 200, updated);
            }
            case "DELETE" -> {
                gameService.delete(id);
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
