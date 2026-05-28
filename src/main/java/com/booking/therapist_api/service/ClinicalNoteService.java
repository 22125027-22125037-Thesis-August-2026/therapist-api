package com.booking.therapist_api.service;

import com.booking.therapist_api.dto.ClinicalNoteDetailResponseDto;
import com.booking.therapist_api.dto.ClinicalNoteListItemDto;
import com.booking.therapist_api.dto.ClinicalNoteRequestDto;
import com.booking.therapist_api.dto.ClinicalNoteResponseDto;
import com.booking.therapist_api.dto.ClinicalNoteUpdateRequestDto;
import com.booking.therapist_api.entity.Appointment;
import com.booking.therapist_api.entity.ClinicalNote;
import com.booking.therapist_api.enums.AppointmentStatus;
import com.booking.therapist_api.enums.ClinicalNoteStatus;
import com.booking.therapist_api.exception.ClinicalNoteAlreadyExistsException;
import com.booking.therapist_api.exception.InvalidAppointmentStateException;
import com.booking.therapist_api.exception.ResourceNotFoundException;
import com.booking.therapist_api.repository.AppointmentRepository;
import com.booking.therapist_api.repository.ClinicalNoteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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

        ClinicalNoteStatus targetStatus =
                request.status() == null ? ClinicalNoteStatus.FINALIZED : request.status();

        if (clinicalNoteRepository.existsByAppointment_Id(appointment.getId())) {
            throw new ClinicalNoteAlreadyExistsException(
                    "Clinical note already exists for appointment id: " + appointment.getId());
        }

        // FINALIZED requires the appointment to be IN_PROGRESS (preserve the
        // original single-step submit semantics).
        if (targetStatus == ClinicalNoteStatus.FINALIZED
                && appointment.getStatus() != AppointmentStatus.IN_PROGRESS) {
            throw new InvalidAppointmentStateException(
                    "Clinical note can only be FINALIZED when appointment is IN_PROGRESS. Current status: "
                            + appointment.getStatus().name());
        }

        // Drafts require an appointment that is still open (UPCOMING / IN_PROGRESS).
        if (targetStatus == ClinicalNoteStatus.DRAFT
                && (appointment.getStatus() == AppointmentStatus.COMPLETED
                    || appointment.getStatus() == AppointmentStatus.CANCELLED)) {
            throw new InvalidAppointmentStateException(
                    "Cannot create a DRAFT note for a "
                            + appointment.getStatus().name() + " appointment.");
        }

        ClinicalNote note = new ClinicalNote();
        note.setAppointment(appointment);
        applyContent(note, request);
        note.setStatus(targetStatus);

        ClinicalNote savedNote = clinicalNoteRepository.save(note);

        if (targetStatus == ClinicalNoteStatus.FINALIZED) {
            appointment.setStatus(AppointmentStatus.COMPLETED);
            appointmentRepository.save(appointment);
        }

        return new ClinicalNoteResponseDto(
                savedNote.getNoteId(),
                appointment.getId(),
                appointment.getStatus().name(),
                savedNote.getStatus().name(),
                savedNote.getCreatedAt(),
                targetStatus == ClinicalNoteStatus.DRAFT
                        ? "Clinical note draft saved"
                        : "Clinical note submitted successfully"
        );
    }

    @Transactional
    public ClinicalNoteDetailResponseDto updateNote(UUID noteId, ClinicalNoteUpdateRequestDto request) {
        ClinicalNote note = clinicalNoteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Clinical note not found for id: " + noteId));

        if (note.getStatus() == ClinicalNoteStatus.FINALIZED) {
            throw new InvalidAppointmentStateException(
                    "FINALIZED notes cannot be edited.");
        }

        if (request.diagnosis() != null)          note.setDiagnosis(request.diagnosis());
        if (request.recommendations() != null)    note.setRecommendations(request.recommendations());
        if (request.subjective() != null)         note.setSubjective(request.subjective());
        if (request.objective() != null)          note.setObjective(request.objective());
        if (request.assessment() != null)         note.setAssessment(request.assessment());
        if (request.plan() != null)               note.setPlan(request.plan());
        if (request.summary() != null)            note.setSummary(request.summary());
        if (request.riskSuicidalIdeation() != null) note.setRiskSuicidalIdeation(request.riskSuicidalIdeation());
        if (request.riskSelfHarm() != null)         note.setRiskSelfHarm(request.riskSelfHarm());
        if (request.riskSubstanceUse() != null)     note.setRiskSubstanceUse(request.riskSubstanceUse());
        if (request.riskAbuse() != null)            note.setRiskAbuse(request.riskAbuse());

        if (request.status() == ClinicalNoteStatus.FINALIZED) {
            finalizeInternal(note);
        }

        ClinicalNote saved = clinicalNoteRepository.save(note);
        return toDetailDto(saved);
    }

    @Transactional
    public ClinicalNoteDetailResponseDto finalizeNote(UUID noteId) {
        ClinicalNote note = clinicalNoteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Clinical note not found for id: " + noteId));

        if (note.getStatus() == ClinicalNoteStatus.FINALIZED) {
            return toDetailDto(note);
        }

        finalizeInternal(note);
        ClinicalNote saved = clinicalNoteRepository.save(note);
        return toDetailDto(saved);
    }

    private void finalizeInternal(ClinicalNote note) {
        Appointment appt = note.getAppointment();
        if (appt.getStatus() != AppointmentStatus.IN_PROGRESS
                && appt.getStatus() != AppointmentStatus.COMPLETED) {
            throw new InvalidAppointmentStateException(
                    "Clinical note can only be FINALIZED when appointment is IN_PROGRESS or COMPLETED. Current status: "
                            + appt.getStatus().name());
        }
        note.setStatus(ClinicalNoteStatus.FINALIZED);
        if (appt.getStatus() == AppointmentStatus.IN_PROGRESS) {
            appt.setStatus(AppointmentStatus.COMPLETED);
            appointmentRepository.save(appt);
        }
    }

    @Transactional(readOnly = true)
    public ClinicalNoteDetailResponseDto getNoteForAppointment(UUID appointmentId) {
        if (!appointmentRepository.existsById(appointmentId)) {
            throw new ResourceNotFoundException(
                    "Appointment not found for id: " + appointmentId);
        }
        ClinicalNote note = clinicalNoteRepository.findByAppointment_Id(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Clinical note not found for appointment id: " + appointmentId));
        return toDetailDto(note);
    }

    @Transactional(readOnly = true)
    public ClinicalNoteDetailResponseDto getNoteById(UUID noteId) {
        ClinicalNote note = clinicalNoteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Clinical note not found for id: " + noteId));
        return toDetailDto(note);
    }

    @Transactional(readOnly = true)
    public Page<ClinicalNoteListItemDto> searchNotes(
            UUID therapistId,
            UUID patientId,
            ClinicalNoteStatus status,
            Pageable pageable
    ) {
        return clinicalNoteRepository.search(therapistId, patientId, status, pageable)
                .map(note -> {
                    Appointment appt = note.getAppointment();
                    return new ClinicalNoteListItemDto(
                            note.getNoteId(),
                            appt.getId(),
                            appt.getProfileId(),
                            appt.getPatientName(),
                            appt.getTherapist().getTherapistId(),
                            note.getStatus().name(),
                            note.getSummary(),
                            appt.getStartDatetime(),
                            note.getUpdatedAt()
                    );
                });
    }

    private void applyContent(ClinicalNote note, ClinicalNoteRequestDto request) {
        note.setDiagnosis(request.diagnosis());
        note.setRecommendations(request.recommendations());
        note.setSubjective(request.subjective());
        note.setObjective(request.objective());
        note.setAssessment(request.assessment());
        note.setPlan(request.plan());
        note.setSummary(request.summary());
        note.setRiskSuicidalIdeation(Boolean.TRUE.equals(request.riskSuicidalIdeation()));
        note.setRiskSelfHarm(Boolean.TRUE.equals(request.riskSelfHarm()));
        note.setRiskSubstanceUse(Boolean.TRUE.equals(request.riskSubstanceUse()));
        note.setRiskAbuse(Boolean.TRUE.equals(request.riskAbuse()));
    }

    private ClinicalNoteDetailResponseDto toDetailDto(ClinicalNote note) {
        Appointment appt = note.getAppointment();
        return new ClinicalNoteDetailResponseDto(
                note.getNoteId(),
                appt.getId(),
                appt.getProfileId(),
                appt.getTherapist().getTherapistId(),
                appt.getStatus().name(),
                note.getStatus().name(),
                note.getDiagnosis(),
                note.getRecommendations(),
                note.getSubjective(),
                note.getObjective(),
                note.getAssessment(),
                note.getPlan(),
                note.getSummary(),
                new ClinicalNoteDetailResponseDto.RiskFlags(
                        note.isRiskSuicidalIdeation(),
                        note.isRiskSelfHarm(),
                        note.isRiskSubstanceUse(),
                        note.isRiskAbuse()
                ),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
