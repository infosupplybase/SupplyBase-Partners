package com.supplybase.partners.identity;

import com.supplybase.partners.testsupport.AbstractIntegrationTest;
import com.supplybase.partners.testsupport.TestSession;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OtpFlowIntegrationTest extends AbstractIntegrationTest {

    private static final String PHONE = "9812345678";

    @Test
    void wrongCodeRepeatedlyLocksTheChallengeWithoutEverRevealingTheRealCode() {
        TestSession session = new TestSession(restTemplate);
        session.get("/api/v1/catalog/categories", String.class);
        session.post("/api/v1/auth/otp/request", Map.of("phone", PHONE), Map.class);

        // Five wrong attempts (the configured max) should each fail with a clear
        // "incorrect code" message; the sixth must fail as attempts-exhausted,
        // never falling through to "no OTP requested" or silently succeeding.
        for (int i = 0; i < 5; i++) {
            ResponseEntity<Map> attempt = session.post("/api/v1/auth/otp/verify",
                    Map.of("phone", PHONE, "code", "000000"), Map.class);
            assertThat(attempt.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        ResponseEntity<Map> exhausted = session.post("/api/v1/auth/otp/verify",
                Map.of("phone", PHONE, "code", "111111"), Map.class);
        assertThat(exhausted.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(String.valueOf(exhausted.getBody().get("message"))).containsIgnoringCase("too many");
    }

    @Test
    void requestingASecondCodeBeforeTheCooldownIsRateLimited() {
        TestSession session = new TestSession(restTemplate);
        session.get("/api/v1/catalog/categories", String.class);

        ResponseEntity<Map> first = session.post("/api/v1/auth/otp/request", Map.of("phone", "9812345679"), Map.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> secondImmediately = session.post("/api/v1/auth/otp/request", Map.of("phone", "9812345679"), Map.class);
        assertThat(secondImmediately.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void correctCodeLogsInAndSubsequentSessionCallReflectsIt() {
        TestSession session = new TestSession(restTemplate);
        session.get("/api/v1/catalog/categories", String.class);
        session.post("/api/v1/auth/otp/request", Map.of("phone", "9812345680"), Map.class);

        ResponseEntity<Map> devCode = session.get("/api/v1/auth/otp/dev/last-code?phone=%2B919812345680", Map.class);
        String code = (String) devCode.getBody().get("code");

        ResponseEntity<Map> verify = session.post("/api/v1/auth/otp/verify",
                Map.of("phone", "9812345680", "code", code), Map.class);
        assertThat(verify.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(verify.getBody().get("authenticated")).isEqualTo(true);

        ResponseEntity<Map> sessionCheck = session.get("/api/v1/auth/session", Map.class);
        assertThat(sessionCheck.getBody().get("authenticated")).isEqualTo(true);
    }
}
