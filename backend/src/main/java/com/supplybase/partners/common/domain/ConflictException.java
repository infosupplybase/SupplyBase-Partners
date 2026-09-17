package com.supplybase.partners.common.domain;

/** Optimistic-lock losses, double-booking, duplicate acceptance, etc. -> HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
