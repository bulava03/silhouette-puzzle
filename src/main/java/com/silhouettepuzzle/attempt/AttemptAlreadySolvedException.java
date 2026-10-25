package com.silhouettepuzzle.attempt;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Thrown when a solution is submitted for an attempt that is already solved;
 * rendered as a 409 problem detail.
 */
public class AttemptAlreadySolvedException extends ErrorResponseException {

    /**
     * Creates the exception.
     *
     * @param id the attempt identifier
     */
    public AttemptAlreadySolvedException(String id) {
        super(HttpStatus.CONFLICT,
                ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Attempt already solved: " + id), null);
    }
}
