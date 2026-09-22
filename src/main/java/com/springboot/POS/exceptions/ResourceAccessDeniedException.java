package com.springboot.POS.exceptions;

/**
 * Ownership / scope violation. Mapped to HTTP 403 by {@link GlobalExceptionHandler},
 * unlike plain {@link UserException} which maps to 400.
 */
public class ResourceAccessDeniedException extends UserException {
    public ResourceAccessDeniedException(String message) {
        super(message);
    }
}
