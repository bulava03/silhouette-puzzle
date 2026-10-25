package com.silhouettepuzzle.attempt;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Thrown when an attempt does not exist; rendered as a 404 problem detail.
 */
public class AttemptNotFoundException extends ErrorResponseException {

    /**
     * Creates the exception for a missing attempt.
     *
     * @param id the requested attempt identifier
     */
    public AttemptNotFoundException(String id) {
        super(HttpStatus.NOT_FOUND,
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Attempt not found: " + id), null);
    }
}
