package app;

import app.http.GameHandler;
import app.http.SessionHandler;
import app.http.UserHandler;
import app.repository.GameRepository;
import app.repository.GamingSessionRepository;
import app.repository.UserRepository;
import app.service.GameService;
import app.service.GamingSessionService;
import app.service.UserService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {

    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        UserRepository userRepository = new UserRepository();
        GameRepository gameRepository = new GameRepository();
        GamingSessionRepository sessionRepository = new GamingSessionRepository();

        UserService userService = new UserService(userRepository, sessionRepository);
        GameService gameService = new GameService(gameRepository, sessionRepository);
        GamingSessionService sessionService = new GamingSessionService(sessionRepository, userRepository, gameRepository);

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        server.createContext("/ping", new PingHandler());
        server.createContext("/users", new UserHandler(userService));
        server.createContext("/games", new GameHandler(gameService));
        server.createContext("/sessions", new SessionHandler(sessionService));

        server.start();
        System.out.println("Cloud Gaming Server started on port: " + PORT);
    }
}
