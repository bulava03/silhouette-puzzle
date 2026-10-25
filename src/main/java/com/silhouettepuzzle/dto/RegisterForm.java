package com.silhouettepuzzle.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The registration form: a username of 3 to 20 Latin letters, digits or
 * underscores, and a password of at least 8 characters.
 */
public record RegisterForm(
        @NotNull @Pattern(regexp = "[A-Za-z0-9_]{3,20}",
                message = "must be 3 to 20 Latin letters, digits or underscores") String username,
        @NotNull @Size(min = 8, max = 72) String password) {
}
