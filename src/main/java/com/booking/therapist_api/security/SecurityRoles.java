package com.booking.therapist_api.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Role-matching helpers that tolerate authorities supplied with or without the
 * Spring {@code ROLE_} prefix. The auth service issues role claims as bare names
 * (e.g. {@code THERAPIST}), so ownership checks must ignore the prefix on both
 * sides rather than assume a normalized {@code ROLE_THERAPIST} authority.
 */
final class SecurityRoles {

    private static final String ROLE_PREFIX = "ROLE_";

    private SecurityRoles() {
    }

    /**
     * True when the authentication carries the given role, comparing names with
     * the {@code ROLE_} prefix stripped from both the requested role and each
     * granted authority.
     */
    static boolean hasRole(Authentication authentication, String role) {
        if (authentication == null || role == null) {
            return false;
        }
        String target = stripPrefix(role);
        for (GrantedAuthority granted : authentication.getAuthorities()) {
            if (target.equals(stripPrefix(granted.getAuthority()))) {
                return true;
            }
        }
        return false;
    }

    private static String stripPrefix(String authority) {
        if (authority == null) {
            return "";
        }
        return authority.startsWith(ROLE_PREFIX) ? authority.substring(ROLE_PREFIX.length()) : authority;
    }
}
