package com.booking.therapist_api.security;

import com.booking.therapist_api.entity.Appointment;
import com.booking.therapist_api.entity.Therapist;
import com.booking.therapist_api.exception.ResourceNotFoundException;
import com.booking.therapist_api.repository.AppointmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicalNoteAuthorizationTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private ClinicalNoteAuthorization clinicalNoteAuthorization;

    @Test
    void canSubmit_allowsAssignedTherapist() {
        UUID appointmentId = UUID.randomUUID();
        UUID therapistId = UUID.randomUUID();

        Appointment appointment = buildAppointment(appointmentId, therapistId, UUID.randomUUID());
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        Authentication authentication = auth(therapistId, "ROLE_THERAPIST");

        assertTrue(clinicalNoteAuthorization.canSubmit(authentication, appointmentId));
    }

    @Test
    void canSubmit_deniesUnassignedTherapist() {
        UUID appointmentId = UUID.randomUUID();
        UUID therapistId = UUID.randomUUID();

        Appointment appointment = buildAppointment(appointmentId, UUID.randomUUID(), UUID.randomUUID());
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        Authentication authentication = auth(therapistId, "ROLE_THERAPIST");

        assertFalse(clinicalNoteAuthorization.canSubmit(authentication, appointmentId));
    }

    @Test
    void canSubmit_allowsAdmin() {
        UUID appointmentId = UUID.randomUUID();
        Appointment appointment = buildAppointment(appointmentId, UUID.randomUUID(), UUID.randomUUID());
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        Authentication authentication = auth(UUID.randomUUID(), "ROLE_ADMIN");

        assertTrue(clinicalNoteAuthorization.canSubmit(authentication, appointmentId));
    }

    @Test
    void canSubmit_deniesPatient() {
        UUID appointmentId = UUID.randomUUID();
        Appointment appointment = buildAppointment(appointmentId, UUID.randomUUID(), UUID.randomUUID());
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        Authentication authentication = auth(UUID.randomUUID(), "ROLE_PATIENT");

        assertFalse(clinicalNoteAuthorization.canSubmit(authentication, appointmentId));
    }

    @Test
    void canSubmit_throwsWhenAppointmentMissing() {
        UUID appointmentId = UUID.randomUUID();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        Authentication authentication = auth(UUID.randomUUID(), "ROLE_THERAPIST");

        assertThrows(
            ResourceNotFoundException.class,
            () -> clinicalNoteAuthorization.canSubmit(authentication, appointmentId)
        );
    }

    @Test
    void canView_allowsPatientOwner() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        Appointment appointment = buildAppointment(appointmentId, UUID.randomUUID(), patientId);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        Authentication authentication = auth(patientId, "ROLE_PATIENT");

        assertTrue(clinicalNoteAuthorization.canView(authentication, appointmentId));
    }

    @Test
    void canView_allowsAssignedTherapist() {
        UUID appointmentId = UUID.randomUUID();
        UUID therapistId = UUID.randomUUID();

        Appointment appointment = buildAppointment(appointmentId, therapistId, UUID.randomUUID());
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        Authentication authentication = auth(therapistId, "ROLE_THERAPIST");

        assertTrue(clinicalNoteAuthorization.canView(authentication, appointmentId));
    }

    @Test
    void canView_allowsAdmin() {
        UUID appointmentId = UUID.randomUUID();
        Appointment appointment = buildAppointment(appointmentId, UUID.randomUUID(), UUID.randomUUID());
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        Authentication authentication = auth(UUID.randomUUID(), "ROLE_ADMIN");

        assertTrue(clinicalNoteAuthorization.canView(authentication, appointmentId));
    }

    @Test
    void canView_deniesOtherPatient() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        Appointment appointment = buildAppointment(appointmentId, UUID.randomUUID(), patientId);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        Authentication authentication = auth(UUID.randomUUID(), "ROLE_PATIENT");

        assertFalse(clinicalNoteAuthorization.canView(authentication, appointmentId));
    }

    @Test
    void canView_throwsWhenAppointmentMissing() {
        UUID appointmentId = UUID.randomUUID();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        Authentication authentication = auth(UUID.randomUUID(), "ROLE_ADMIN");

        assertThrows(
            ResourceNotFoundException.class,
            () -> clinicalNoteAuthorization.canView(authentication, appointmentId)
        );
    }

    private Appointment buildAppointment(UUID appointmentId, UUID therapistId, UUID patientId) {
        Therapist therapist = new Therapist();
        therapist.setTherapistId(therapistId);

        Appointment appointment = new Appointment();
        appointment.setId(appointmentId);
        appointment.setTherapist(therapist);
        appointment.setProfileId(patientId);
        return appointment;
    }

    private Authentication auth(UUID principalId, String role) {
        return new UsernamePasswordAuthenticationToken(
            principalId.toString(),
            null,
            List.of(new SimpleGrantedAuthority(role))
        );
    }
}
