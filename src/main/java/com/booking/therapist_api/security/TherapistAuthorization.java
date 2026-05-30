package com.booking.therapist_api.security;

import com.booking.therapist_api.entity.Appointment;
import com.booking.therapist_api.enums.AssignmentStatus;
import com.booking.therapist_api.exception.ResourceNotFoundException;
import com.booking.therapist_api.repository.AppointmentRepository;
import com.booking.therapist_api.repository.TherapistAssignmentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Authorization helpers for therapist-facing endpoints. Used from
 * @PreAuthorize expressions as `@therapistAuthorization.<method>(...)`.
 *
 * Principal contract (see SecurityConfig + JwtAuthenticationFilter):
 *   - authentication.name is the JWT principal UUID (therapist id for ROLE_THERAPIST,
 *     patient profile id for ROLE_PATIENT).
 *   - ROLE_ADMIN bypasses ownership checks.
 */
@Component("therapistAuthorization")
public class TherapistAuthorization {

    private final TherapistAssignmentRepository assignmentRepository;
    private final AppointmentRepository appointmentRepository;

    public TherapistAuthorization(
            TherapistAssignmentRepository assignmentRepository,
            AppointmentRepository appointmentRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.appointmentRepository = appointmentRepository;
    }

    /**
     * Allowed when the caller is the same therapist, or has ROLE_ADMIN.
     */
    public boolean isSelfTherapistOrAdmin(Authentication authentication, UUID therapistId) {
        if (authentication == null || therapistId == null) {
            return false;
        }
        if (hasRole(authentication, "ROLE_ADMIN")) {
            return true;
        }
        if (!hasRole(authentication, "ROLE_THERAPIST")) {
            return false;
        }
        return therapistId.toString().equals(authentication.getName());
    }

    /**
     * Allowed when the caller is the therapist who owns the appointment, the patient on it,
     * or has ROLE_ADMIN.
     */
    public boolean canViewAppointment(Authentication authentication, UUID appointmentId) {
        if (authentication == null || appointmentId == null) {
            return false;
        }
        if (hasRole(authentication, "ROLE_ADMIN")) {
            return true;
        }

        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Appointment not found for id: " + appointmentId));

        UUID callerId = parseUuidOrNull(authentication.getName());
        if (callerId == null) {
            return false;
        }

        if (hasRole(authentication, "ROLE_THERAPIST")) {
            return appt.getTherapist().getTherapistId().equals(callerId);
        }
        if (hasRole(authentication, "ROLE_PATIENT")) {
            return appt.getProfileId().equals(callerId);
        }
        return false;
    }

    /**
     * Allowed only when the caller is the therapist who owns the appointment, or has ROLE_ADMIN.
     * Used for therapist-only mutations (confirm / reject).
     */
    public boolean canMutateAppointment(Authentication authentication, UUID appointmentId) {
        if (authentication == null || appointmentId == null) {
            return false;
        }
        if (hasRole(authentication, "ROLE_ADMIN")) {
            return true;
        }
        if (!hasRole(authentication, "ROLE_THERAPIST")) {
            return false;
        }

        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Appointment not found for id: " + appointmentId));

        UUID callerId = parseUuidOrNull(authentication.getName());
        return callerId != null && appt.getTherapist().getTherapistId().equals(callerId);
    }

    /**
     * Cancel is allowed by the owning therapist, the owning patient, or ROLE_ADMIN.
     */
    public boolean canCancelAppointment(Authentication authentication, UUID appointmentId) {
        if (authentication == null || appointmentId == null) {
            return false;
        }
        if (hasRole(authentication, "ROLE_ADMIN")) {
            return true;
        }

        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Appointment not found for id: " + appointmentId));

        UUID callerId = parseUuidOrNull(authentication.getName());
        if (callerId == null) {
            return false;
        }
        if (hasRole(authentication, "ROLE_THERAPIST")) {
            return appt.getTherapist().getTherapistId().equals(callerId);
        }
        if (hasRole(authentication, "ROLE_PATIENT")) {
            return appt.getProfileId().equals(callerId);
        }
        return false;
    }

    /**
     * Allowed when the caller is the therapist currently assigned (status=ACTIVE) to the patient,
     * or has ROLE_ADMIN.
     */
    public boolean canAccessPatient(Authentication authentication, UUID profileId) {
        if (authentication == null || profileId == null) {
            return false;
        }
        if (hasRole(authentication, "ROLE_ADMIN")) {
            return true;
        }
        if (!hasRole(authentication, "ROLE_THERAPIST")) {
            return false;
        }

        UUID callerId = parseUuidOrNull(authentication.getName());
        if (callerId == null) {
            return false;
        }

        return assignmentRepository.findByProfileIdAndStatus(profileId, AssignmentStatus.ACTIVE)
                .map(assignment -> assignment.getTherapist().getTherapistId().equals(callerId))
                .orElse(false);
    }

    private static boolean hasRole(Authentication auth, String role) {
        return SecurityRoles.hasRole(auth, role);
    }

    private static UUID parseUuidOrNull(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
