package com.olliejw.cmdwebhooks.exceptions;

public class RateLimitException extends Exception {
    private final long retryAfterMs;

    public RateLimitException(String message, long retryAfterMs) {
        super(message);
        this.retryAfterMs = retryAfterMs;
    }

    public long getRetryAfterMs() {
        return retryAfterMs;
    }
}
