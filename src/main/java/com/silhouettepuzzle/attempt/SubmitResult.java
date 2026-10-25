package com.silhouettepuzzle.attempt;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.silhouettepuzzle.geometry.CheckResult;

/**
 * The verdict on a submitted solution.
 *
 * @param solved        whether the solution was accepted
 * @param overlapRatio  overlap between pieces, as a fraction of the silhouette area
 * @param mismatchRatio difference from the silhouette, as a fraction of its area
 * @param timeMs        server-measured time; only present when solved
 * @param moves         recorded moves; only present when solved
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubmitResult(boolean solved, double overlapRatio, double mismatchRatio, Long timeMs, Integer moves) {

    /**
     * Creates the verdict for a rejected solution.
     *
     * @param check the checker result
     * @return the verdict without time and moves
     */
    public static SubmitResult unsolved(CheckResult check) {
        return new SubmitResult(false, check.overlapRatio(), check.mismatchRatio(), null, null);
    }

    /**
     * Creates the verdict for an accepted solution.
     *
     * @param check  the checker result
     * @param timeMs server-measured time
     * @param moves  recorded moves
     * @return the verdict with time and moves
     */
    public static SubmitResult solved(CheckResult check, long timeMs, int moves) {
        return new SubmitResult(true, check.overlapRatio(), check.mismatchRatio(), timeMs, moves);
    }
}
