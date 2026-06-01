package com.american2day.skyshard;

public class ShardException extends Exception {
    public ShardException(String message) {
        super(message);
    }
    public ShardException(String message, Throwable cause) {
        super(message, cause);
    }
}
