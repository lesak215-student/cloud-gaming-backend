package app.controller;

import app.dto.StartSessionRequest;
import app.dto.UpdateSessionRequest;
import app.model.GamingSession;
import app.model.SessionStatus;
import app.service.GamingSessionService;
import jakarta.validation.Valid;
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
@RequestMapping("/sessions")
public class GamingSessionController {

    private final GamingSessionService sessionService;

    public GamingSessionController(GamingSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<GamingSession> start(@Valid @RequestBody StartSessionRequest request) {
        GamingSession session = sessionService.start(request.userId, request.gameId, request.resolution);
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    @GetMapping
    public List<GamingSession> getAll(@RequestParam(required = false) Long userId,
                                      @RequestParam(required = false) Long gameId,
                                      @RequestParam(required = false) SessionStatus status) {
        return sessionService.getAll(userId, gameId, status);
    }

    @GetMapping("/{id}")
    public GamingSession getById(@PathVariable Long id) {
        return sessionService.getById(id);
    }

    @PutMapping("/{id}")
    public GamingSession update(@PathVariable Long id, @Valid @RequestBody UpdateSessionRequest request) {
        return sessionService.update(id, request.resolution, request.notes);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sessionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pause")
    public GamingSession pause(@PathVariable Long id) {
        return sessionService.pause(id);
    }

    @PostMapping("/{id}/resume")
    public GamingSession resume(@PathVariable Long id) {
        return sessionService.resume(id);
    }

    @PostMapping("/{id}/terminate")
    public GamingSession terminate(@PathVariable Long id) {
        return sessionService.terminate(id);
    }
}
