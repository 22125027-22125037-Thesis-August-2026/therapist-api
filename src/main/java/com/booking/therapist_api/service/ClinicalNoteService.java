package com.booking.therapist_api.service;

import com.booking.therapist_api.dto.ClinicalNoteRequestDto;
import com.booking.therapist_api.dto.ClinicalNoteResponseDto;
import com.booking.therapist_api.entity.Appointment;
import com.booking.therapist_api.entity.ClinicalNote;
import com.booking.therapist_api.enums.AppointmentStatus;
import com.booking.therapist_api.exception.ClinicalNoteAlreadyExistsException;
import com.booking.therapist_api.exception.InvalidAppointmentStateException;
import com.booking.therapist_api.exception.ResourceNotFoundException;
import com.booking.therapist_api.repository.AppointmentRepository;
import com.booking.therapist_api.repository.ClinicalNoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClinicalNoteService {

    private final AppointmentRepository appointmentRepository;
    private final ClinicalNoteRepository clinicalNoteRepository;

    public ClinicalNoteService(AppointmentRepository appointmentRepository,
                               ClinicalNoteRepository clinicalNoteRepository) {
        this.appointmentRepository = appointmentRepository;
        this.clinicalNoteRepository = clinicalNoteRepository;
    }

    @Transactional
    public ClinicalNoteResponseDto submitNote(ClinicalNoteRequestDto request) {
        Appointment appointment = appointmentRepository.findById(request.appointmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Appointment not found for id: " + request.appointmentId()));

        if (appointment.getStatus() != AppointmentStatus.IN_PROGRESS) {
            throw new InvalidAppointmentStateException(
                    "Clinical note can only be submitted when appointment is IN_PROGRESS. Current status: "
                            + appointment.getStatus().name());
        }

        if (clinicalNoteRepository.existsByAppointment_Id(appointment.getId())) {
            throw new ClinicalNoteAlreadyExistsException(
                    "Clinical note already exists for appointment id: " + appointment.getId());
        }

        ClinicalNote note = new ClinicalNote();
        note.setAppointment(appointment);
        note.setDiagnosis(request.diagnosis());
        note.setRecommendations(request.recommendations());

        ClinicalNote savedNote = clinicalNoteRepository.save(note);

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);

        return new ClinicalNoteResponseDto(
                savedNote.getNoteId(),
                appointment.getId(),
                appointment.getStatus().name(),
                savedNote.getCreatedAt(),
                "Clinical note submitted successfully"
        );
    }
}
