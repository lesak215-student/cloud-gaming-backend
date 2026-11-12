package app.controller;

import app.dto.CreateGameRequest;
import app.dto.UpdateGameRequest;
import app.model.Game;
import app.model.SubscriptionTier;
import app.service.GameService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    public ResponseEntity<Game> create(@RequestBody CreateGameRequest request) {
        int maxSlots = request.maxSlots != null ? request.maxSlots : 0;
        Game game = gameService.create(request.title, request.genre, request.requiredTier, maxSlots);
        return ResponseEntity.status(HttpStatus.CREATED).body(game);
    }

    @GetMapping
    public List<Game> getAll(@RequestParam(required = false) String genre,
                             @RequestParam(required = false) SubscriptionTier tier,
                             @RequestParam(required = false) Boolean hasSlots) {
        return gameService.getAll(genre, tier, hasSlots);
    }

    @GetMapping("/{id}")
    public Game getById(@PathVariable Long id) {
        return gameService.getById(id);
    }

    @PutMapping("/{id}")
    public Game update(@PathVariable Long id, @RequestBody UpdateGameRequest request) {
        int maxSlots = request.maxSlots != null ? request.maxSlots : 0;
        return gameService.update(id, request.title, request.genre, request.requiredTier, maxSlots);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        gameService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
