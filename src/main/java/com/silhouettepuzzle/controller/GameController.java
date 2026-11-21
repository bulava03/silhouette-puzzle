package com.silhouettepuzzle.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.silhouettepuzzle.dto.AttemptStarted;
import com.silhouettepuzzle.dto.LevelView;
import com.silhouettepuzzle.dto.SubmitRequest;
import com.silhouettepuzzle.dto.SubmitResult;
import com.silhouettepuzzle.security.PlayerPrincipal;
import com.silhouettepuzzle.service.AttemptService;
import com.silhouettepuzzle.service.LevelService;

import jakarta.validation.Valid;

/**
 * What the game asks for as the player moves between levels without leaving
 * the page: a level to build, a timed attempt on it, and the check.
 */
@RestController
public class GameController {

    private final AttemptService attempts;
    private final LevelService levels;
    private final LevelViews views;

    public GameController(AttemptService attempts, LevelService levels, LevelViews views) {
        this.attempts = attempts;
        this.levels = levels;
        this.views = views;
    }

    @GetMapping("/api/levels/{levelId}")
    public LevelView level(@PathVariable String levelId) {
        return views.view(levelId);
    }

    /** Starts the clock on a level for a logged-in player; guests are not timed. */
    @PostMapping("/api/levels/{levelId}/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public AttemptStarted start(@PathVariable String levelId, @AuthenticationPrincipal PlayerPrincipal player) {
        return new AttemptStarted(attempts.start(levels.get(levelId), player).id());
    }

    @PostMapping("/api/levels/{levelId}/check")
    public SubmitResult check(@PathVariable String levelId, @Valid @RequestBody SubmitRequest request,
                              @AuthenticationPrincipal PlayerPrincipal player) {
        return attempts.submit(levelId, request, player);
    }
}
