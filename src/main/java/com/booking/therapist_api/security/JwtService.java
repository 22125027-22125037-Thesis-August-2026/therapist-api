package com.booking.therapist_api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
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

    private final PublicKey verificationKey;
    private final String expectedIssuer;
    private final String expectedAudience;
    private final String expectedSigningKid;

    public JwtService(
            @Value("${jwt.public-key:${JWT_PUBLIC_KEY:}}") String jwtPublicKey,
            @Value("${jwt.issuer:${JWT_ISSUER:mhsa-auth}}") String expectedIssuer,
            @Value("${jwt.audience:${JWT_AUDIENCE:mhsa-api}}") String expectedAudience,
            @Value("${jwt.signing-kid:${JWT_SIGNING_KID:}}") String expectedSigningKid
    ) {
        this.verificationKey = parseRsaPublicKey(jwtPublicKey);
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
