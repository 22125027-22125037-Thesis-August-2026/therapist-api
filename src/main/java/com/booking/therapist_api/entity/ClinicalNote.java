package com.booking.therapist_api.entity;

import com.booking.therapist_api.enums.ClinicalNoteStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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

    @Column(name = "subjective", columnDefinition = "TEXT")
    private String subjective;

    @Column(name = "objective", columnDefinition = "TEXT")
    private String objective;

    @Column(name = "assessment", columnDefinition = "TEXT")
    private String assessment;

    @Column(name = "plan", columnDefinition = "TEXT")
    private String plan;

    @Column(name = "summary", length = 500)
    private String summary;

    @Column(name = "risk_suicidal", nullable = false)
    private boolean riskSuicidalIdeation = false;

    @Column(name = "risk_self_harm", nullable = false)
    private boolean riskSelfHarm = false;

    @Column(name = "risk_substance_use", nullable = false)
    private boolean riskSubstanceUse = false;

    @Column(name = "risk_abuse", nullable = false)
    private boolean riskAbuse = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ClinicalNoteStatus status = ClinicalNoteStatus.FINALIZED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now(Clock.systemUTC());
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now(Clock.systemUTC());
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

    public String getSubjective() {
        return subjective;
    }

    public void setSubjective(String subjective) {
        this.subjective = subjective;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getAssessment() {
        return assessment;
    }

    public void setAssessment(String assessment) {
        this.assessment = assessment;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public boolean isRiskSuicidalIdeation() {
        return riskSuicidalIdeation;
    }

    public void setRiskSuicidalIdeation(boolean riskSuicidalIdeation) {
        this.riskSuicidalIdeation = riskSuicidalIdeation;
    }

    public boolean isRiskSelfHarm() {
        return riskSelfHarm;
    }

    public void setRiskSelfHarm(boolean riskSelfHarm) {
        this.riskSelfHarm = riskSelfHarm;
    }

    public boolean isRiskSubstanceUse() {
        return riskSubstanceUse;
    }

    public void setRiskSubstanceUse(boolean riskSubstanceUse) {
        this.riskSubstanceUse = riskSubstanceUse;
    }

    public boolean isRiskAbuse() {
        return riskAbuse;
    }

    public void setRiskAbuse(boolean riskAbuse) {
        this.riskAbuse = riskAbuse;
    }

    public ClinicalNoteStatus getStatus() {
        return status;
    }

    public void setStatus(ClinicalNoteStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
