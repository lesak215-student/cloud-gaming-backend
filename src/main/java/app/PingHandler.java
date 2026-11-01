package app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class PingHandler implements HttpHandler {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            exchange.getResponseHeaders().set("Allow", "GET");
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        byte[] body = mapper.writeValueAsBytes(Map.of("message", "pong"));

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
