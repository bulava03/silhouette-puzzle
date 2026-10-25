package com.silhouettepuzzle.level;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Thrown when a requested level does not exist; rendered as a 404 problem detail.
 */
public class LevelNotFoundException extends ErrorResponseException {

    /**
     * Creates the exception for a missing level.
     *
     * @param id the requested level identifier
     */
    public LevelNotFoundException(String id) {
        super(HttpStatus.NOT_FOUND, ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Level not found: " + id), null);
    }
}
