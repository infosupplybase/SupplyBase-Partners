package com.supplybase.partners.testsupport;

import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.Map;

/** Drives the real HTTP OTP flow to stand up an authenticated partner session for tests. */
public class PartnerTestFlows {

    private final TestRestTemplate restTemplate;

    public PartnerTestFlows(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    public TestSession registerAndLogin(String tenDigitPhone) {
        TestSession session = new TestSession(restTemplate);
        // Any permitAll GET first, so the CSRF cookie is on the jar before the first POST.
        session.get("/api/v1/catalog/categories", String.class);

        session.post("/api/v1/auth/otp/request", Map.of("phone", tenDigitPhone), Map.class);
        ResponseEntity<Map> devCode = session.get("/api/v1/auth/otp/dev/last-code?phone=" + normalizedFor(tenDigitPhone), Map.class);
        String code = (String) devCode.getBody().get("code");

        ResponseEntity<Map> verify = session.post("/api/v1/auth/otp/verify",
                Map.of("phone", tenDigitPhone, "code", code), Map.class);
        if (!verify.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("OTP verify failed: " + verify);
        }
        return session;
    }

    /** URL-safe (percent-encoded '+') so it survives being pasted straight into a query string. */
    private String normalizedFor(String tenDigitPhone) {
        return "%2B91" + tenDigitPhone;
    }
}
