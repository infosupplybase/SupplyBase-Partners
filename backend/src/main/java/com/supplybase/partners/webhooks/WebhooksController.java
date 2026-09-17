package com.supplybase.partners.webhooks;

import com.supplybase.partners.common.domain.BadRequestException;
import com.supplybase.partners.money.MoneyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Signature-verified, replay-safe provider event ingestion. Body shape:
 * {"eventId": "...", "payload": {...}, "signature": "hex(hmac_sha256(secret, eventId + payloadJson))"}.
 * Replay-safety comes from the unique provider_event_id constraint on the
 * downstream event tables (KitOrderEvent/PayoutEvent), not just this check.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhooksController {

    private final WebhookSignatureVerifier verifier;
    private final MoneyService moneyService;
    private final ObjectMapper objectMapper;

    public WebhooksController(WebhookSignatureVerifier verifier, MoneyService moneyService, ObjectMapper objectMapper) {
        this.verifier = verifier;
        this.moneyService = moneyService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/payouts")
    public ResponseEntity<Void> payouts(@RequestBody Map<String, Object> body) {
        verifyOrThrow(body);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) body.get("payload");
        Long payoutId = Long.valueOf(payload.get("payoutId").toString());
        String eventType = payload.get("eventType").toString();
        moneyService.handlePayoutWebhookEvent(payoutId, eventType, body.get("eventId").toString(), toJson(payload));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/kit-payments")
    public ResponseEntity<Void> kitPayments(@RequestBody Map<String, Object> body) {
        verifyOrThrow(body);
        // A real kit-payments provider integration would apply the event to the matching
        // KitOrder here (idempotently, via kit_order_event.provider_event_id) the same way
        // MoneyService#handlePayoutWebhookEvent does for payouts. Not wired to a real
        // provider in this build -- see docs/ASSUMPTIONS.md.
        return ResponseEntity.ok().build();
    }

    private void verifyOrThrow(Map<String, Object> body) {
        Object eventId = body.get("eventId");
        Object payload = body.get("payload");
        Object signature = body.get("signature");
        if (eventId == null || payload == null || signature == null) {
            throw new BadRequestException("eventId, payload and signature are required.");
        }
        String canonical = eventId + toJson(payload);
        if (!verifier.verify(canonical, signature.toString())) {
            throw new BadRequestException("Invalid webhook signature.");
        }
    }

    /** payloadJson columns are typed JSON in MySQL -- Map#toString() ("{k=v}") is not valid JSON and would fail on insert. */
    private String toJson(Object value) {
        return objectMapper.writeValueAsString(value);
    }
}
