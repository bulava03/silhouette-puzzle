package com.silhouettepuzzle.player;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Thrown when a username is already registered; rendered as a 409 problem detail.
 */
public class UsernameTakenException extends ErrorResponseException {

    /**
     * Creates the exception.
     *
     * @param username the requested name
     */
    public UsernameTakenException(String username) {
        super(HttpStatus.CONFLICT,
                ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Username is taken: " + username), null);
    }
}
