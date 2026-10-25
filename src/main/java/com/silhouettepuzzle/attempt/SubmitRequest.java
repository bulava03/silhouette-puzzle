package com.silhouettepuzzle.attempt;

import java.util.List;

import com.silhouettepuzzle.geometry.Placement;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * A solution sent by the client.
 *
 * @param placements placement of each level piece, in level order
 * @param moves      number of moves the player made
 */
public record SubmitRequest(
        @NotNull @Size(min = 1, max = MAX_PIECES) List<@NotNull Placement> placements,
        @PositiveOrZero @Max(MAX_MOVES) int moves) {

    /** Upper bound on pieces in a submission, far above any real level. */
    public static final int MAX_PIECES = 100;

    /** Upper bound on reported moves, to keep junk out of records. */
    public static final int MAX_MOVES = 100_000;
}
