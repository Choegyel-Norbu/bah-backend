package com.attirehub.shared.exception;

/**
 * Thrown when a user attempts to sign in before verifying their email address.
 */
public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
