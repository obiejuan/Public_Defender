package com.cmps115.public_defender;

/**
 * Created by bryan on 5/22/17.
 */

public class StreamException extends Exception {
    public StreamException() { super(); }
    public StreamException(String message) { super(message); }
    public StreamException(String message, Throwable cause) { super(message, cause); }
    public StreamException(Throwable cause) { super(cause); }
}