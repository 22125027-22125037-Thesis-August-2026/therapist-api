package com.booking.therapist_api.service;

import com.booking.therapist_api.dto.UpcomingAppointmentResponseDto;
import com.booking.therapist_api.entity.Appointment;
import com.booking.therapist_api.entity.ScheduleSlot;
import com.booking.therapist_api.entity.Therapist;
import com.booking.therapist_api.enums.AppointmentMode;
import com.booking.therapist_api.enums.AppointmentStatus;
import com.booking.therapist_api.exception.ResourceNotFoundException;
import com.booking.therapist_api.repository.AppointmentRepository;
import com.booking.therapist_api.repository.ScheduleSlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private ScheduleSlotRepository slotRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private BookingEventPublisher eventPublisher;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void getClosestUpcomingAppointment_returnsNearestFutureAppointment() {
        UUID profileId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        UUID therapistId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();

        Therapist therapist = new Therapist();
        therapist.setTherapistId(therapistId);

        ScheduleSlot slot = new ScheduleSlot();
        slot.setId(slotId);

        Appointment appointment = new Appointment();
        appointment.setId(appointmentId);
        appointment.setProfileId(profileId);
        appointment.setTherapist(therapist);
        appointment.setSlot(slot);
        appointment.setMode(AppointmentMode.VIDEO);
        appointment.setStatus(AppointmentStatus.UPCOMING);
        appointment.setStartDatetime(Instant.now().plusSeconds(3600));

        when(appointmentRepository.findFirstByProfileIdAndStartDatetimeAfterAndStatusOrderByStartDatetimeAsc(
                eq(profileId),
                any(Instant.class),
                eq(AppointmentStatus.UPCOMING)
        )).thenReturn(Optional.of(appointment));

        UpcomingAppointmentResponseDto response = bookingService.getClosestUpcomingAppointment(profileId);

        assertEquals(appointmentId, response.appointmentId());
        assertEquals(profileId, response.profileId());
        assertEquals(therapistId, response.therapistId());
        assertEquals(slotId, response.slotId());
        assertEquals("VIDEO", response.mode());
        assertEquals("UPCOMING", response.status());
        assertEquals(appointment.getStartDatetime(), response.startDatetime());
    }

    @Test
    void getClosestUpcomingAppointment_throwsWhenNoFutureAppointmentExists() {
        UUID profileId = UUID.randomUUID();

        when(appointmentRepository.findFirstByProfileIdAndStartDatetimeAfterAndStatusOrderByStartDatetimeAsc(
                eq(profileId),
                any(Instant.class),
                eq(AppointmentStatus.UPCOMING)
        )).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.getClosestUpcomingAppointment(profileId));
    }
}
