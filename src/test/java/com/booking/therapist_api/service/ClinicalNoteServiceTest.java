package com.booking.therapist_api.service;

import com.booking.therapist_api.dto.ClinicalNoteRequestDto;
import com.booking.therapist_api.dto.ClinicalNoteResponseDto;
import com.booking.therapist_api.entity.Appointment;
import com.booking.therapist_api.entity.ClinicalNote;
import com.booking.therapist_api.entity.Review;
import com.booking.therapist_api.entity.ScheduleSlot;
import com.booking.therapist_api.entity.Therapist;
import com.booking.therapist_api.enums.AppointmentMode;
import com.booking.therapist_api.enums.AppointmentStatus;
import com.booking.therapist_api.enums.ClinicalNoteStatus;
import com.booking.therapist_api.exception.ClinicalNoteNotAllowedException;
import com.booking.therapist_api.exception.InvalidAppointmentStateException;
import com.booking.therapist_api.repository.AppointmentRepository;
import com.booking.therapist_api.repository.ClinicalNoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicalNoteServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ClinicalNoteRepository clinicalNoteRepository;

    @InjectMocks
    private ClinicalNoteService clinicalNoteService;

    @Test
    void submitNote_finalizedFromInProgress_setsAppointmentProfessionalComplete() {
        Appointment appointment = appointment(AppointmentStatus.IN_PROGRESS);
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(clinicalNoteRepository.existsByAppointment_Id(appointment.getId())).thenReturn(false);
        lenient().when(clinicalNoteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ClinicalNoteResponseDto response =
                clinicalNoteService.submitNote(noteRequest(appointment.getId(), ClinicalNoteStatus.FINALIZED));

        assertEquals(AppointmentStatus.PROFESSIONAL_COMPLETE, appointment.getStatus());
        assertEquals(AppointmentStatus.PROFESSIONAL_COMPLETE.name(), response.appointmentStatus());
    }

    @Test
    void submitNote_finalizedFromPatientCompleteWithinGraceWindow_setsAppointmentOverallComplete() {
        Appointment appointment = appointment(AppointmentStatus.PATIENT_COMPLETE);
        attachReview(appointment, Instant.now().minusSeconds(3600));
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(clinicalNoteRepository.existsByAppointment_Id(appointment.getId())).thenReturn(false);
        lenient().when(clinicalNoteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        clinicalNoteService.submitNote(noteRequest(appointment.getId(), ClinicalNoteStatus.FINALIZED));

        assertEquals(AppointmentStatus.OVERALL_COMPLETE, appointment.getStatus());
    }

    @Test
    void submitNote_finalizedFromPatientCompleteAfterGraceWindow_throwsClinicalNoteNotAllowed() {
        Appointment appointment = appointment(AppointmentStatus.PATIENT_COMPLETE);
        attachReview(appointment, Instant.now().minus(Duration.ofHours(25)));
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(clinicalNoteRepository.existsByAppointment_Id(appointment.getId())).thenReturn(false);

        assertThrows(ClinicalNoteNotAllowedException.class,
                () -> clinicalNoteService.submitNote(noteRequest(appointment.getId(), ClinicalNoteStatus.FINALIZED)));
    }

    @Test
    void submitNote_finalizedFromOverallComplete_throwsInvalidState() {
        Appointment appointment = appointment(AppointmentStatus.OVERALL_COMPLETE);
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(clinicalNoteRepository.existsByAppointment_Id(appointment.getId())).thenReturn(false);

        assertThrows(InvalidAppointmentStateException.class,
                () -> clinicalNoteService.submitNote(noteRequest(appointment.getId(), ClinicalNoteStatus.FINALIZED)));
    }

    @Test
    void finalizeNote_draftWithinGraceWindow_setsAppointmentOverallComplete() {
        Appointment appointment = appointment(AppointmentStatus.PATIENT_COMPLETE);
        attachReview(appointment, Instant.now().minusSeconds(3600));
        ClinicalNote draft = draftNote(appointment);
        when(clinicalNoteRepository.findById(draft.getNoteId())).thenReturn(Optional.of(draft));
        lenient().when(clinicalNoteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        clinicalNoteService.finalizeNote(draft.getNoteId());

        assertEquals(AppointmentStatus.OVERALL_COMPLETE, appointment.getStatus());
        assertEquals(ClinicalNoteStatus.FINALIZED, draft.getStatus());
    }

    @Test
    void finalizeNote_draftAfterGraceWindowExpired_throwsClinicalNoteNotAllowed() {
        Appointment appointment = appointment(AppointmentStatus.PATIENT_COMPLETE);
        attachReview(appointment, Instant.now().minus(Duration.ofHours(25)));
        ClinicalNote draft = draftNote(appointment);
        when(clinicalNoteRepository.findById(draft.getNoteId())).thenReturn(Optional.of(draft));

        assertThrows(ClinicalNoteNotAllowedException.class,
                () -> clinicalNoteService.finalizeNote(draft.getNoteId()));
    }

    private Appointment appointment(AppointmentStatus status) {
        Therapist therapist = new Therapist();
        therapist.setTherapistId(UUID.randomUUID());

        ScheduleSlot slot = new ScheduleSlot();
        slot.setId(UUID.randomUUID());

        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setProfileId(UUID.randomUUID());
        appointment.setTherapist(therapist);
        appointment.setSlot(slot);
        appointment.setMode(AppointmentMode.VIDEO);
        appointment.setStatus(status);
        appointment.setStartDatetime(Instant.now().minusSeconds(7200));
        return appointment;
    }

    private void attachReview(Appointment appointment, Instant reviewedAt) {
        Review review = new Review();
        review.setReviewId(UUID.randomUUID());
        review.setAppointment(appointment);
        review.setRating(5);
        review.setCreatedAt(reviewedAt);
        appointment.setReview(review);
    }

    private ClinicalNote draftNote(Appointment appointment) {
        ClinicalNote note = new ClinicalNote();
        note.setNoteId(UUID.randomUUID());
        note.setAppointment(appointment);
        note.setStatus(ClinicalNoteStatus.DRAFT);
        return note;
    }

    private ClinicalNoteRequestDto noteRequest(UUID appointmentId, ClinicalNoteStatus status) {
        return new ClinicalNoteRequestDto(
                appointmentId,
                "Diagnosis", "Recommendations", null, null, null, null, null,
                false, false, false, false,
                status
        );
    }
}
