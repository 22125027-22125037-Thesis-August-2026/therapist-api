package com.booking.therapist_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clinical_notes")
public class ClinicalNote {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "note_id", nullable = false, updatable = false)
    private UUID noteId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appt_id", nullable = false, unique = true)
    private Appointment appointment;

    @Column(name = "diagnosis", columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "recommendations", columnDefinition = "TEXT")
    private String recommendations;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        // Instant stores UTC timeline points; use UTC clock explicitly on creation.
        if (createdAt == null) {
            createdAt = Instant.now(Clock.systemUTC());
        }
    }

    public ClinicalNote() {
    }

    public UUID getNoteId() {
        return noteId;
    }

    public void setNoteId(UUID noteId) {
        this.noteId = noteId;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(String recommendations) {
        this.recommendations = recommendations;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
