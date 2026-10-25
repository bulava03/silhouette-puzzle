package com.silhouettepuzzle.attempt;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.silhouettepuzzle.geometry.CheckResult;

/**
 * The verdict on a submission; time and moves are present only when solved.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubmitResult(boolean solved, double overlapRatio, double mismatchRatio, Long timeMs, Integer moves) {

    static SubmitResult of(CheckResult check, Long timeMs, Integer moves) {
        return new SubmitResult(check.solved(), check.overlapRatio(), check.mismatchRatio(), timeMs, moves);
    }
}
