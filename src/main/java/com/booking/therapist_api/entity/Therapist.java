package com.booking.therapist_api.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

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

    @Column(name = "gender")
    private String gender;

    @Column(name = "is_lgbtq_allied")
    private Boolean isLgbtqAllied;

    @Column(name = "communication_style")
    private String communicationStyle;

    // Maps PostgreSQL varchar[] directly to Java String[].
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "treated_challenges", columnDefinition = "varchar[]")
    private String[] treatedChallenges;

    @OneToOne(mappedBy = "therapist", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private TherapistZoomCredential zoomCredential;

    @OneToMany(mappedBy = "therapist", fetch = FetchType.LAZY)
    private Set<WeeklyTemplate> weeklyTemplates = new HashSet<>();

    @OneToMany(mappedBy = "therapist", fetch = FetchType.LAZY)
    private Set<ScheduleSlot> scheduleSlots = new HashSet<>();

    @OneToMany(mappedBy = "therapist", fetch = FetchType.LAZY)
    private Set<TherapistAssignment> therapistAssignments = new HashSet<>();

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

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Boolean getIsLgbtqAllied() {
        return isLgbtqAllied;
    }

    public void setIsLgbtqAllied(Boolean isLgbtqAllied) {
        this.isLgbtqAllied = isLgbtqAllied;
    }

    public String getCommunicationStyle() {
        return communicationStyle;
    }

    public void setCommunicationStyle(String communicationStyle) {
        this.communicationStyle = communicationStyle;
    }

    public String[] getTreatedChallenges() {
        return treatedChallenges;
    }

    public void setTreatedChallenges(String[] treatedChallenges) {
        this.treatedChallenges = treatedChallenges;
    }

    public TherapistZoomCredential getZoomCredential() {
        return zoomCredential;
    }

    public void setZoomCredential(TherapistZoomCredential zoomCredential) {
        this.zoomCredential = zoomCredential;
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

    public Set<TherapistAssignment> getTherapistAssignments() {
        return therapistAssignments;
    }

    public void setTherapistAssignments(Set<TherapistAssignment> therapistAssignments) {
        this.therapistAssignments = therapistAssignments;
    }
}
