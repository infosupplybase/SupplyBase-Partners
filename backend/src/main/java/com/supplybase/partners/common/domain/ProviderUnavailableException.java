package com.supplybase.partners.common.domain;

/** A configured provider mode has no working adapter -- fail clearly, never silently no-op. */
public class ProviderUnavailableException extends RuntimeException {
    public ProviderUnavailableException(String message) {
        super(message);
    }
}
