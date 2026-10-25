package com.silhouettepuzzle.player;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Registration form.
 *
 * @param username 3 to 20 Latin letters, digits or underscores
 * @param password at least 8 characters; at most 72 bytes in UTF-8, the BCrypt limit
 */
public record RegisterRequest(
        @NotNull @Pattern(regexp = "[A-Za-z0-9_]{3,20}",
                message = "must be 3 to 20 Latin letters, digits or underscores") String username,
        @NotNull @Size(min = 8, max = 72) String password) {
}
