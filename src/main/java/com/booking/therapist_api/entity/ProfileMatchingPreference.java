package com.booking.therapist_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "profiles_preferences")
public class ProfileMatchingPreference {

    // Cross-domain Auth/Profile reference: keep as scalar UUID, no JPA relationship.
    @Id
    @Column(name = "profile_id", nullable = false, updatable = false)
    private UUID profileId;

    @Column(name = "has_prior_counseling")
    private String hasPriorCounseling;

    @Column(name = "sexual_orientation")
    private String sexualOrientation;

    @Column(name = "is_lgbtq_priority")
    private Boolean isLgbtqPriority;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "reasons", columnDefinition = "varchar[]")
    private String[] reasons;

    @Column(name = "communication_style")
    private String communicationStyle;

    @UpdateTimestamp
    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    public ProfileMatchingPreference() {
    }

    public UUID getProfileId() {
        return profileId;
    }

    public void setProfileId(UUID profileId) {
        this.profileId = profileId;
    }

    public String getHasPriorCounseling() {
        return hasPriorCounseling;
    }

    public void setHasPriorCounseling(String hasPriorCounseling) {
        this.hasPriorCounseling = hasPriorCounseling;
    }

    public String getSexualOrientation() {
        return sexualOrientation;
    }

    public void setSexualOrientation(String sexualOrientation) {
        this.sexualOrientation = sexualOrientation;
    }

    public Boolean getIsLgbtqPriority() {
        return isLgbtqPriority;
    }

    public void setIsLgbtqPriority(Boolean isLgbtqPriority) {
        this.isLgbtqPriority = isLgbtqPriority;
    }

    public String[] getReasons() {
        return reasons;
    }

    public void setReasons(String[] reasons) {
        this.reasons = reasons;
    }

    public String getCommunicationStyle() {
        return communicationStyle;
    }

    public void setCommunicationStyle(String communicationStyle) {
        this.communicationStyle = communicationStyle;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(Instant lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
}
