package com.booking.therapist_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "schedule_slots")
public class ScheduleSlot {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "slot_id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "therapist_id", nullable = false)
    private Therapist therapist;

    @Column(name = "start_datetime", nullable = false)
    private Instant startDatetime;

    @Column(name = "end_datetime", nullable = false)
    private Instant endDatetime;

    @Column(name = "is_booked", nullable = false)
    private boolean isBooked = false;

    @OneToOne(mappedBy = "slot", fetch = FetchType.LAZY)
    private Appointment appointment;

    public ScheduleSlot() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Therapist getTherapist() {
        return therapist;
    }

    public void setTherapist(Therapist therapist) {
        this.therapist = therapist;
    }

    public Instant getStartDatetime() {
        return startDatetime;
    }

    public void setStartDatetime(Instant startDatetime) {
        this.startDatetime = startDatetime;
    }

    public Instant getEndDatetime() {
        return endDatetime;
    }

    public void setEndDatetime(Instant endDatetime) {
        this.endDatetime = endDatetime;
    }

    public boolean isBooked() {
        return isBooked;
    }

    public void setBooked(boolean booked) {
        isBooked = booked;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }
}
