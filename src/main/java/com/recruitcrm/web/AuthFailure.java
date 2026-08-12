package com.recruitcrm.web;

/**
 * Thrown by AuthUtil after it has ALREADY sent a 401/403 response, purely
 * to unwind out of the handler.
 *
 * It exists as its own type so handlers can catch "the user wasn't allowed
 * in" without also catching real failures. The previous code caught plain
 * IllegalStateException here, which silently swallowed genuine database
 * errors too — the request just died with no response and nothing logged.
 */
public class AuthFailure extends RuntimeException {
    public AuthFailure(String message) {
        super(message);
    }
}
