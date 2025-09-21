package com.olliejw.cmdwebhooks;

public class RateLimitException extends Exception {
    private final int retryAfter;

    public RateLimitException(String message, int retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }

    public int getRetryAfter() {
        return retryAfter;
    }
}
