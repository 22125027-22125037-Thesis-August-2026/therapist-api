package com.booking.therapist_api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Collection;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;

@Service
public class JwtService {

    private static final String PROFILE_ID_CLAIM = "profileId";
    private static final String ROLE_CLAIM = "role";

    private static final String EMAIL_CLAIM = "email";
    private static final String NAME_CLAIM = "name";
    private static final String PREFERRED_USERNAME_CLAIM = "preferred_username";
    private static final String GIVEN_NAME_CLAIM = "given_name";
    private static final String FAMILY_NAME_CLAIM = "family_name";

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtService.class);

    private final PublicKey verificationKey;
    private final JwksVerificationKeyLocator jwksKeyLocator;
    private final String expectedIssuer;
    private final String expectedAudience;
    private final String expectedSigningKid;

    /**
     * Prefers Auth's published key set over a statically configured public key.
     *
     * <p>With a JWKS URI the verification key is selected per token by its {@code kid}, so a
     * key rotation at Auth needs no redeploy here. The static-key branch remains for a
     * deployment that has no JWKS endpoint to point at.
     */
    public JwtService(
            @Value("${jwt.jwks-uri:${JWT_JWKS_URI:}}") String jwksUri,
            @Value("${jwt.public-key:${JWT_PUBLIC_KEY:}}") String jwtPublicKey,
            @Value("${jwt.issuer:${JWT_ISSUER:mhsa-auth}}") String expectedIssuer,
            @Value("${jwt.audience:${JWT_AUDIENCE:mhsa-api}}") String expectedAudience,
            @Value("${jwt.signing-kid:${JWT_SIGNING_KID:}}") String expectedSigningKid,
            ObjectMapper objectMapper
    ) {
        if (StringUtils.hasText(jwksUri)) {
            if (StringUtils.hasText(jwtPublicKey)) {
                LOGGER.warn("Both jwt.jwks-uri and jwt.public-key are configured; "
                        + "the static key is ignored in favour of the published key set");
            }
            this.jwksKeyLocator = new JwksVerificationKeyLocator(jwksUri, objectMapper);
            this.verificationKey = null;
            LOGGER.info("JWT verification keys will be resolved from JWKS at {}", jwksUri);
        } else {
            this.jwksKeyLocator = null;
            this.verificationKey = parseRsaPublicKey(jwtPublicKey);
            LOGGER.info("JWT verification configured with a static RSA public key");
        }

        this.expectedIssuer = expectedIssuer;
        this.expectedAudience = expectedAudience;
        this.expectedSigningKid = expectedSigningKid;
    }

    public String extractPrincipalId(String token) {
        Claims claims = parseClaims(token);
        String profileId = claims.get(PROFILE_ID_CLAIM, String.class);
        return StringUtils.hasText(profileId) ? profileId : claims.getSubject();
    }

    public String extractRole(String token) {
        String rawRole = parseClaims(token).get(ROLE_CLAIM, String.class);
        return normalizeRole(rawRole);
    }

    public String extractEmail(String token) {
        Claims claims = parseClaims(token);
        String email = claims.get(EMAIL_CLAIM, String.class);
        if (StringUtils.hasText(email)) {
            return email;
        }

        // Some identity providers use preferred_username for email/username.
        String preferred = claims.get(PREFERRED_USERNAME_CLAIM, String.class);
        return StringUtils.hasText(preferred) ? preferred : null;
    }

    public String extractDisplayName(String token) {
        Claims claims = parseClaims(token);

        String name = claims.get(NAME_CLAIM, String.class);
        if (StringUtils.hasText(name)) {
            return name;
        }

        String given = claims.get(GIVEN_NAME_CLAIM, String.class);
        String family = claims.get(FAMILY_NAME_CLAIM, String.class);
        String combined = (StringUtils.hasText(given) ? given.trim() : "")
                + (StringUtils.hasText(family) ? (StringUtils.hasText(given) ? " " : "") + family.trim() : "");

        return StringUtils.hasText(combined) ? combined : null;
    }

    public boolean isTokenValid(String token) {
        try {
            Jws<Claims> jws = parseJws(token);
            Claims claims = jws.getPayload();
            Date expiration = claims.getExpiration();
            return (expiration == null || expiration.toInstant().isAfter(Instant.now()))
                    && isExpectedIssuer(claims)
                    && isExpectedAudience(claims)
                    && isExpectedSigningKid(jws);
        } catch (Exception ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return parseJws(token).getPayload();
    }

    private Jws<Claims> parseJws(String token) {
        if (jwksKeyLocator != null) {
            return Jwts.parser()
                    .keyLocator(jwksKeyLocator)
                    .build()
                    .parseSignedClaims(token);
        }

        return Jwts.parser()
                .verifyWith(verificationKey)
                .build()
                .parseSignedClaims(token);
    }

    private boolean isExpectedIssuer(Claims claims) {
        return !StringUtils.hasText(expectedIssuer) || expectedIssuer.equals(claims.getIssuer());
    }

    private boolean isExpectedAudience(Claims claims) {
        if (!StringUtils.hasText(expectedAudience)) {
            return true;
        }

        Object audienceClaim = claims.get("aud");
        if (audienceClaim instanceof String audience) {
            return expectedAudience.equals(audience);
        }
        if (audienceClaim instanceof Collection<?> audiences) {
            return audiences.stream().anyMatch(expectedAudience::equals);
        }
        return false;
    }

    private boolean isExpectedSigningKid(Jws<Claims> jws) {
        // Under JWKS the kid *is* the key-selection mechanism: the locator already refused any
        // token whose kid it could not resolve from the published set. Additionally pinning one
        // kid here would reject the new key the moment Auth rotates — the precise failure this
        // migration exists to remove — so the pin is deliberately ignored in that mode.
        if (jwksKeyLocator != null) {
            return true;
        }

        if (!StringUtils.hasText(expectedSigningKid)) {
            return true;
        }
        return expectedSigningKid.equals(jws.getHeader().getKeyId());
    }

    private PublicKey parseRsaPublicKey(String base64PublicKey) {
        if (!StringUtils.hasText(base64PublicKey)) {
            throw new IllegalStateException("JWT public key is not configured. Set JWT_PUBLIC_KEY or jwt.public-key.");
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(base64PublicKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePublic(keySpec);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse JWT RSA public key.", ex);
        }
    }

    private String normalizeRole(String rawRole) {
        if (!StringUtils.hasText(rawRole)) {
            return null;
        }

        String normalized = rawRole.trim().toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "TEEN", "PATIENT", "ROLE_PATIENT" -> "ROLE_PATIENT";
            case "THERAPIST", "ROLE_THERAPIST" -> "ROLE_THERAPIST";
            default -> normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
        };
    }
}
