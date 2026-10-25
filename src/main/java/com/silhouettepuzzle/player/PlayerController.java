package com.silhouettepuzzle.player;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.silhouettepuzzle.config.OpenApiConfig;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Registration and the logged-in player. Login and logout are Spring
 * Security's {@code /login} and {@code /logout}.
 */
@RestController
@Tag(name = "Players", description = "Registration and the logged-in player")
public class PlayerController {

    private final PlayerService players;
    private final PlayerRepository repository;

    public PlayerController(PlayerService players, PlayerRepository repository) {
        this.players = players;
        this.repository = repository;
    }

    @PostMapping("/api/players")
    @Operation(summary = "Register", description = "Usernames are unique regardless of case.")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        Player player = players.register(request);
        return ResponseEntity.created(URI.create("/api/players/" + player.id()))
                .body(Map.of("id", player.id(), "username", player.username()));
    }

    @GetMapping("/api/me")
    @Operation(summary = "Get the logged-in player", security = @SecurityRequirement(name = OpenApiConfig.SESSION))
    public Map<String, String> me(@Parameter(hidden = true) @AuthenticationPrincipal PlayerPrincipal me) {
        return Map.of("id", me.getId(), "username", me.getUsername());
    }

    @GetMapping("/api/me/progress")
    @Operation(summary = "Get the best run on every solved level",
            security = @SecurityRequirement(name = OpenApiConfig.SESSION))
    public List<PlayerBest> progress(@Parameter(hidden = true) @AuthenticationPrincipal PlayerPrincipal me) {
        return repository.findById(me.getId()).map(Player::bestsOrEmpty).orElse(List.of()).stream()
                .sorted(Comparator.comparing(PlayerBest::levelId))
                .toList();
    }
}
