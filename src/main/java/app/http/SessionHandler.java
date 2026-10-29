package app.http;

import app.dto.StartSessionRequest;
import app.dto.UpdateSessionRequest;
import app.model.GamingSession;
import app.model.SessionStatus;
import app.service.GamingSessionService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SessionHandler implements HttpHandler {

    private final GamingSessionService sessionService;

    public SessionHandler(GamingSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            String subPath = extractSubPath(path, "/sessions");

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
                Long userIdFilter = HttpUtils.parseOptionalLong(params, "userId");
                Long gameIdFilter = HttpUtils.parseOptionalLong(params, "gameId");
                SessionStatus statusFilter = HttpUtils.parseOptionalStatus(params, "status");

                List<GamingSession> sessions = sessionService.getAll(userIdFilter, gameIdFilter, statusFilter);
                HttpUtils.sendJson(exchange, 200, sessions);
            }
            case "POST" -> {
                StartSessionRequest request = HttpUtils.parseBody(exchange, StartSessionRequest.class);
                GamingSession session = sessionService.start(request.userId, request.gameId, request.resolution);
                HttpUtils.sendJson(exchange, 201, session);
            }
            default -> {
                exchange.getResponseHeaders().set("Allow", "GET, POST");
                HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            }
        }
    }

    private void handleItem(HttpExchange exchange, String method, String subPath) throws IOException {
        String[] parts = subPath.split("/");

        if (parts.length == 1) {
            Long id = HttpUtils.parsePathId(parts[0]);
            switch (method) {
                case "GET" -> {
                    GamingSession session = sessionService.getById(id);
                    HttpUtils.sendJson(exchange, 200, session);
                }
                case "PUT" -> {
                    UpdateSessionRequest request = HttpUtils.parseBody(exchange, UpdateSessionRequest.class);
                    GamingSession updated = sessionService.update(id, request.resolution);
                    HttpUtils.sendJson(exchange, 200, updated);
                }
                case "DELETE" -> {
                    sessionService.delete(id);
                    HttpUtils.sendNoContent(exchange);
                }
                default -> {
                    exchange.getResponseHeaders().set("Allow", "GET, PUT, DELETE");
                    HttpUtils.sendError(exchange, 405, "Method Not Allowed");
                }
            }
            return;
        }

        if (parts.length == 2) {
            Long id = HttpUtils.parsePathId(parts[0]);
            String action = parts[1];

            if (!"POST".equals(method)) {
                exchange.getResponseHeaders().set("Allow", "POST");
                HttpUtils.sendError(exchange, 405, "Method Not Allowed");
                return;
            }

            switch (action) {
                case "pause" -> {
                    GamingSession session = sessionService.pause(id);
                    HttpUtils.sendJson(exchange, 200, session);
                }
                case "resume" -> {
                    GamingSession session = sessionService.resume(id);
                    HttpUtils.sendJson(exchange, 200, session);
                }
                case "terminate" -> {
                    GamingSession session = sessionService.terminate(id);
                    HttpUtils.sendJson(exchange, 200, session);
                }
                default -> HttpUtils.sendError(exchange, 404, "Unknown action: " + action);
            }
            return;
        }

        HttpUtils.sendError(exchange, 404, "Unknown path: " + exchange.getRequestURI().getPath());
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
