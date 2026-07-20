package com.booking.therapist_api.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Locator;
import io.jsonwebtoken.ProtectedHeader;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves RS256 verification keys from Auth's published key set, cached by {@code kid}.
 *
 * <p>therapist-api is a standalone repository and does not share the backend monorepo's
 * {@code shared-jwt} module, so this mirrors that module's {@code JwksKeyProvider} semantics
 * against jjwt's {@link Locator} SPI rather than reusing it.
 *
 * <p>Two deliberate choices, both carried over:
 *
 * <ul>
 *   <li><b>Fetch is lazy, not at startup.</b> This service boots fine while Auth is still
 *       coming up; it simply cannot validate tokens until the first fetch succeeds. Fetching
 *       eagerly would turn Auth into a hard startup dependency, which is a worse failure mode
 *       than a few early 401s.
 *   <li><b>Refresh is rate-limited.</b> An unknown kid triggers a refetch, since that is what
 *       a rotation looks like from here. Without a floor on the interval, a stream of tokens
 *       carrying forged kids would turn into a request amplifier pointed at Auth. The floor is
 *       far shorter while nothing is cached, so a service that started before Auth was
 *       reachable recovers in seconds rather than rejecting everything for a minute.
 * </ul>
 */
public final class JwksVerificationKeyLocator implements Locator<Key> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwksVerificationKeyLocator.class);

    private static final Duration MIN_REFRESH_INTERVAL = Duration.ofSeconds(60);
    private static final Duration EMPTY_CACHE_RETRY_INTERVAL = Duration.ofSeconds(5);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final String endpoint;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private volatile Map<String, PublicKey> keysByKid = Map.of();
    private volatile Instant lastFetchAttempt = Instant.EPOCH;

    public JwksVerificationKeyLocator(String endpoint, ObjectMapper objectMapper) {
        this.endpoint = endpoint;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    @Override
    public Key locate(Header header) {
        String kid = header instanceof ProtectedHeader protectedHeader
                ? protectedHeader.getKeyId()
                : null;

        if (kid == null) {
            throw new JwtException("RS256 token has no kid header; cannot select a key from the JWKS");
        }

        PublicKey cached = keysByKid.get(kid);
        if (cached != null) {
            return cached;
        }

        refreshIfDue();

        PublicKey refreshed = keysByKid.get(kid);
        if (refreshed == null) {
            throw new JwtException("No published JWKS key matches kid " + kid);
        }
        return refreshed;
    }

    private synchronized void refreshIfDue() {
        Duration floor = keysByKid.isEmpty() ? EMPTY_CACHE_RETRY_INTERVAL : MIN_REFRESH_INTERVAL;

        // Re-check inside the lock: while this thread waited, another may have just fetched.
        if (Instant.now().isBefore(lastFetchAttempt.plus(floor))) {
            return;
        }
        lastFetchAttempt = Instant.now();

        try {
            keysByKid = fetch();
            LOGGER.info("Refreshed JWKS from {}: {} key(s) — {}",
                    endpoint, keysByKid.size(), keysByKid.keySet());
        } catch (Exception ex) {
            // Keep serving whatever is already cached. A transient Auth outage should not
            // invalidate keys that are still perfectly good.
            LOGGER.warn("Could not refresh JWKS from {}: {} — keeping {} cached key(s)",
                    endpoint, ex.getMessage(), keysByKid.size());
        }
    }

    private Map<String, PublicKey> fetch() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("JWKS endpoint returned HTTP " + response.statusCode());
        }

        JsonNode keys = objectMapper.readTree(response.body()).path("keys");
        if (!keys.isArray() || keys.isEmpty()) {
            throw new IllegalStateException("JWKS endpoint returned an empty key set");
        }

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        Map<String, PublicKey> parsed = new HashMap<>();

        for (JsonNode key : keys) {
            String kid = key.path("kid").asText(null);
            if (!"RSA".equals(key.path("kty").asText()) || kid == null) {
                LOGGER.debug("Skipping unusable JWKS entry: kty={} kid={}", key.path("kty").asText(), kid);
                continue;
            }
            // Signum 1 forces an unsigned reading, so a publisher that leaves BigInteger's
            // two's-complement sign byte on the modulus still parses correctly here.
            BigInteger modulus = new BigInteger(1, decode(key.path("n").asText()));
            BigInteger exponent = new BigInteger(1, decode(key.path("e").asText()));
            parsed.put(kid, keyFactory.generatePublic(new RSAPublicKeySpec(modulus, exponent)));
        }

        if (parsed.isEmpty()) {
            throw new IllegalStateException("JWKS endpoint returned no usable RSA keys");
        }
        return Map.copyOf(parsed);
    }

    private static byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}
