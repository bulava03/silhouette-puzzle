package com.silhouettepuzzle.attempt;

import java.util.List;

import com.silhouettepuzzle.geometry.Placement;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * A solution sent by the client: one placement per level piece, in level
 * order, and the number of moves the player made.
 */
public record SubmitRequest(
        @NotNull @Size(min = 1, max = 100) List<@NotNull Placement> placements,
        @PositiveOrZero @Max(100_000) int moves) {
}
