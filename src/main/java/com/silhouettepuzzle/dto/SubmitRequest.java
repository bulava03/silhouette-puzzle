package com.silhouettepuzzle.dto;

import java.util.List;

import com.silhouettepuzzle.geometry.Placement;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A picture sent for checking: one placement per level piece, in level
 * order. {@code attemptId} is the timed attempt the page got for a
 * logged-in player; guests send none and get no time.
 */
public record SubmitRequest(
        String attemptId,
        @NotNull @Size(min = 1, max = 100) List<@NotNull Placement> placements) {
}
