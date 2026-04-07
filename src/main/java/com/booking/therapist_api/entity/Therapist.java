package com.booking.therapist_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "therapists")
public class Therapist {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "therapist_id", nullable = false, updatable = false)
    private UUID therapistId;

    // Auth domain reference must stay as plain UUID (cross-microservice).
    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "specialization")
    private String specialization;

    @Column(name = "country")
    private String country;

    @Column(name = "years_experience")
    private Integer yearsExperience;

    @Column(name = "about_me", columnDefinition = "TEXT")
    private String aboutMe;

    @Column(name = "rating_avg", precision = 3, scale = 2)
    private BigDecimal ratingAvg;

    @Column(name = "license_url")
    private String licenseUrl;

    @OneToMany(mappedBy = "therapist", fetch = FetchType.LAZY)
    private Set<WeeklyTemplate> weeklyTemplates = new HashSet<>();

    @OneToMany(mappedBy = "therapist", fetch = FetchType.LAZY)
    private Set<ScheduleSlot> scheduleSlots = new HashSet<>();

    public Therapist() {
    }

    public UUID getTherapistId() {
        return therapistId;
    }

    public void setTherapistId(UUID therapistId) {
        this.therapistId = therapistId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Integer getYearsExperience() {
        return yearsExperience;
    }

    public void setYearsExperience(Integer yearsExperience) {
        this.yearsExperience = yearsExperience;
    }

    public String getAboutMe() {
        return aboutMe;
    }

    public void setAboutMe(String aboutMe) {
        this.aboutMe = aboutMe;
    }

    public BigDecimal getRatingAvg() {
        return ratingAvg;
    }

    public void setRatingAvg(BigDecimal ratingAvg) {
        this.ratingAvg = ratingAvg;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }

    public Set<WeeklyTemplate> getWeeklyTemplates() {
        return weeklyTemplates;
    }

    public void setWeeklyTemplates(Set<WeeklyTemplate> weeklyTemplates) {
        this.weeklyTemplates = weeklyTemplates;
    }

    public Set<ScheduleSlot> getScheduleSlots() {
        return scheduleSlots;
    }

    public void setScheduleSlots(Set<ScheduleSlot> scheduleSlots) {
        this.scheduleSlots = scheduleSlots;
    }
}
