package com.booking.therapist_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "therapist_zoom_credentials")
public class TherapistZoomCredential {

    @Id
    @Column(name = "therapist_id", nullable = false, updatable = false)
    private UUID therapistId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "therapist_id")
    private Therapist therapist;

    @Column(name = "zoom_email", nullable = false, unique = true)
    private String zoomEmail;

    @Column(name = "zoom_meeting_number", nullable = false, length = 50)
    private String zoomMeetingNumber;

    @Column(name = "zoom_meeting_password", nullable = false, length = 50)
    private String zoomMeetingPassword;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public TherapistZoomCredential() {
    }

    public UUID getTherapistId() {
        return therapistId;
    }

    public void setTherapistId(UUID therapistId) {
        this.therapistId = therapistId;
    }

    public Therapist getTherapist() {
        return therapist;
    }

    public void setTherapist(Therapist therapist) {
        this.therapist = therapist;
    }

    public String getZoomEmail() {
        return zoomEmail;
    }

    public void setZoomEmail(String zoomEmail) {
        this.zoomEmail = zoomEmail;
    }

    public String getZoomMeetingNumber() {
        return zoomMeetingNumber;
    }

    public void setZoomMeetingNumber(String zoomMeetingNumber) {
        this.zoomMeetingNumber = zoomMeetingNumber;
    }

    public String getZoomMeetingPassword() {
        return zoomMeetingPassword;
    }

    public void setZoomMeetingPassword(String zoomMeetingPassword) {
        this.zoomMeetingPassword = zoomMeetingPassword;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
