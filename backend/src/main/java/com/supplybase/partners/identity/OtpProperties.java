package com.supplybase.partners.identity;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "supplybase.otp")
public class OtpProperties {
    private int length = 6;
    private int ttlSeconds = 300;
    private int resendCooldownSeconds = 30;
    private int maxAttempts = 5;
    private int maxRequestsPerPhonePerHour = 8;

    public int getLength() { return length; }
    public void setLength(int length) { this.length = length; }
    public int getTtlSeconds() { return ttlSeconds; }
    public void setTtlSeconds(int ttlSeconds) { this.ttlSeconds = ttlSeconds; }
    public int getResendCooldownSeconds() { return resendCooldownSeconds; }
    public void setResendCooldownSeconds(int resendCooldownSeconds) { this.resendCooldownSeconds = resendCooldownSeconds; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public int getMaxRequestsPerPhonePerHour() { return maxRequestsPerPhonePerHour; }
    public void setMaxRequestsPerPhonePerHour(int v) { this.maxRequestsPerPhonePerHour = v; }
}
