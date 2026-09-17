package com.supplybase.partners.webhooks;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * HMAC-SHA256 verification for inbound provider webhooks. A real deployment
 * wires this to each provider's actual signing scheme (Razorpay, Stripe,
 * etc.); here it verifies against one shared secret as a working stand-in,
 * documented as an extension point in docs/ASSUMPTIONS.md.
 */
@Component
public class WebhookSignatureVerifier {

    private final String sharedSecret;

    public WebhookSignatureVerifier(@Value("${supplybase.webhooks.shared-secret}") String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }

    public boolean verify(String canonicalPayload, String providedSignatureHex) {
        if (providedSignatureHex == null || providedSignatureHex.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(sharedSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computed = mac.doFinal(canonicalPayload.getBytes(StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(computed);
            return MessageDigest.isEqual(computedHex.getBytes(StandardCharsets.UTF_8), providedSignatureHex.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
}
