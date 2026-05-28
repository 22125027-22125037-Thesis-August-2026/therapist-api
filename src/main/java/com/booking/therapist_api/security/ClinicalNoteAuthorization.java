package com.booking.therapist_api.security;

import com.booking.therapist_api.entity.Appointment;
import com.booking.therapist_api.entity.ClinicalNote;
import com.booking.therapist_api.exception.ResourceNotFoundException;
import com.booking.therapist_api.repository.AppointmentRepository;
import com.booking.therapist_api.repository.ClinicalNoteRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("clinicalNoteAuthorization")
public class ClinicalNoteAuthorization {

    private final AppointmentRepository appointmentRepository;
    private final ClinicalNoteRepository clinicalNoteRepository;

    public ClinicalNoteAuthorization(
            AppointmentRepository appointmentRepository,
            ClinicalNoteRepository clinicalNoteRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.clinicalNoteRepository = clinicalNoteRepository;
    }

    public boolean canSubmit(Authentication authentication, UUID appointmentId) {
        AuthorizationContext context = buildContext(authentication);
        if (context == null) {
            return false;
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Appointment not found for id: " + appointmentId));

        if (context.isAdmin) {
            return true;
        }
        if (!context.isTherapist) {
            return false;
        }
        return appointment.getTherapist().getTherapistId().equals(context.requesterId);
    }

    public boolean canView(Authentication authentication, UUID appointmentId) {
        AuthorizationContext context = buildContext(authentication);
        if (context == null) {
            return false;
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Appointment not found for id: " + appointmentId));

        if (context.isAdmin) {
            return true;
        }
        if (context.isTherapist) {
            return appointment.getTherapist().getTherapistId().equals(context.requesterId);
        }
        if (context.isPatient) {
            return appointment.getProfileId().equals(context.requesterId);
        }
        return false;
    }

    public boolean canViewNote(Authentication authentication, UUID noteId) {
        return resolveNote(noteId).map(note -> canView(authentication, note.getAppointment().getId())).orElse(false);
    }

    public boolean canMutateNote(Authentication authentication, UUID noteId) {
        AuthorizationContext context = buildContext(authentication);
        if (context == null) {
            return false;
        }
        if (context.isAdmin) {
            return true;
        }
        if (!context.isTherapist) {
            return false;
        }
        return resolveNote(noteId)
                .map(note -> note.getAppointment().getTherapist().getTherapistId().equals(context.requesterId))
                .orElse(false);
    }

    private java.util.Optional<ClinicalNote> resolveNote(UUID noteId) {
        return clinicalNoteRepository.findById(noteId);
    }

    private AuthorizationContext buildContext(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }

        UUID requesterId;
        try {
            requesterId = UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            return null;
        }

        boolean isTherapist = authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_THERAPIST".equals(authority.getAuthority()));
        boolean isPatient = authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_PATIENT".equals(authority.getAuthority()));
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        return new AuthorizationContext(requesterId, isTherapist, isPatient, isAdmin);
    }

    private static final class AuthorizationContext {
        private final UUID requesterId;
        private final boolean isTherapist;
        private final boolean isPatient;
        private final boolean isAdmin;

        private AuthorizationContext(UUID requesterId, boolean isTherapist, boolean isPatient, boolean isAdmin) {
            this.requesterId = requesterId;
            this.isTherapist = isTherapist;
            this.isPatient = isPatient;
            this.isAdmin = isAdmin;
        }
    }
}
