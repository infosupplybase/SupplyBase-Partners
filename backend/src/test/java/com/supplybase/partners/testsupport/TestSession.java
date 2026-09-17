package com.supplybase.partners.testsupport;

import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A minimal per-"user" cookie jar + CSRF header pair on top of
 * {@link TestRestTemplate}, so concurrency tests can drive several
 * independently-authenticated sessions against the same running application
 * instance -- exactly how two real partners racing for the same job/slot
 * would look over HTTP.
 */
public class TestSession {

    private static final Pattern COOKIE_PAIR = Pattern.compile("^([^=]+)=([^;]*)");

    private final TestRestTemplate restTemplate;
    private final Map<String, String> cookies = new LinkedHashMap<>();

    public TestSession(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public <T> ResponseEntity<T> get(String path, Class<T> responseType) {
        return exchange(HttpMethod.GET, path, null, responseType);
    }

    public <T> ResponseEntity<T> post(String path, Object body, Class<T> responseType) {
        return exchange(HttpMethod.POST, path, body, responseType);
    }

    public <T> ResponseEntity<T> exchange(HttpMethod method, String path, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        if (!cookies.isEmpty()) {
            headers.add(HttpHeaders.COOKIE, String.join("; ", cookies.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue()).toList()));
        }
        if (method != HttpMethod.GET) {
            String csrf = cookies.get("XSRF-TOKEN");
            if (csrf != null) {
                headers.add("X-XSRF-TOKEN", csrf);
            }
        }
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        ResponseEntity<T> response = restTemplate.exchange(path, method, entity, responseType);
        captureCookies(response);
        return response;
    }

    private void captureCookies(ResponseEntity<?> response) {
        for (String setCookie : response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE)) {
            Matcher m = COOKIE_PAIR.matcher(setCookie);
            if (m.find()) {
                cookies.put(m.group(1), m.group(2));
            }
        }
    }
}
