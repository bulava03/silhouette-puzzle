package com.silhouettepuzzle.attempt;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Thrown when a submission does not fit its level; rendered as a 400 problem detail.
 */
public class InvalidSubmissionException extends ErrorResponseException {

    /**
     * Creates the exception.
     *
     * @param detail what is wrong with the submission
     */
    public InvalidSubmissionException(String detail) {
        super(HttpStatus.BAD_REQUEST, ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail), null);
    }
}
