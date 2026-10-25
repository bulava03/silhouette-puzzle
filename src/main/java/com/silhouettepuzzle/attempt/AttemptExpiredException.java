package com.silhouettepuzzle.attempt;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Thrown when a solution comes for an attempt that ran out of time;
 * rendered as a 410 problem detail.
 */
public class AttemptExpiredException extends ErrorResponseException {

    /**
     * Creates the exception.
     *
     * @param id the attempt identifier
     */
    public AttemptExpiredException(String id) {
        super(HttpStatus.GONE, ProblemDetail.forStatusAndDetail(HttpStatus.GONE,
                "Attempt " + id + " has expired; start the level again"), null);
    }
}
