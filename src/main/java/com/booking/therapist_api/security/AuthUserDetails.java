package com.booking.therapist_api.security;

/**
 * Lightweight user details extracted from JWT claims and attached to the Spring Security Authentication.
 *
 * Note: principal remains the profile/user id string for backwards compatibility with @AuthenticationPrincipal String.
 */
public record AuthUserDetails(String email, String name) {
}