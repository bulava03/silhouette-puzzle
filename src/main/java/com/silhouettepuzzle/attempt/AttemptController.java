package com.silhouettepuzzle.attempt;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * Starting attempts and submitting solutions.
 */
@RestController
public class AttemptController {

    private final AttemptService service;

    /**
     * Creates the controller.
     *
     * @param service attempt logic
     */
    public AttemptController(AttemptService service) {
        this.service = service;
    }

    /**
     * Starts a timed attempt at a level.
     *
     * @param levelId the level to play
     * @return 201 with the attempt
     */
    @PostMapping("/api/levels/{levelId}/attempts")
    public ResponseEntity<StartedAttempt> start(@PathVariable String levelId) {
        Attempt attempt = service.start(levelId);
        return ResponseEntity.created(URI.create("/api/attempts/" + attempt.id())).body(StartedAttempt.of(attempt));
    }

    /**
     * Checks a solution for an attempt.
     *
     * @param attemptId the attempt
     * @param request   placements and move count
     * @return the verdict
     */
    @PostMapping("/api/attempts/{attemptId}/submit")
    public SubmitResult submit(@PathVariable String attemptId, @Valid @RequestBody SubmitRequest request) {
        return service.submit(attemptId, request.placements(), request.moves());
    }
}
