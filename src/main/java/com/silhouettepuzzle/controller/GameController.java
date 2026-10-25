package com.silhouettepuzzle.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.silhouettepuzzle.dto.SubmitRequest;
import com.silhouettepuzzle.dto.SubmitResult;
import com.silhouettepuzzle.security.PlayerPrincipal;
import com.silhouettepuzzle.service.AttemptService;

import jakarta.validation.Valid;

/**
 * The one call the game page makes: checking a picture.
 */
@RestController
public class GameController {

    private final AttemptService attempts;

    public GameController(AttemptService attempts) {
        this.attempts = attempts;
    }

    @PostMapping("/api/levels/{levelId}/check")
    public SubmitResult check(@PathVariable String levelId, @Valid @RequestBody SubmitRequest request,
                              @AuthenticationPrincipal PlayerPrincipal player) {
        return attempts.submit(levelId, request, player);
    }
}
