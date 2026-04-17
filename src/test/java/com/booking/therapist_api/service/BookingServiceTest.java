package com.booking.therapist_api.service;

import com.booking.therapist_api.dto.AppointmentHistoryItemResponseDto;
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
import java.util.List;
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

        @Test
        void getCompletedAndCancelledAppointments_returnsHistoryItems() {
        UUID profileId = UUID.randomUUID();
        UUID completedAppointmentId = UUID.randomUUID();
        UUID cancelledAppointmentId = UUID.randomUUID();

        Therapist therapist = new Therapist();
        UUID therapistId = UUID.randomUUID();
        therapist.setTherapistId(therapistId);
        therapist.setFullName("Dr. Alice Carter");
        therapist.setSpecialization("Anxiety");
        therapist.setCountry("Canada");

        ScheduleSlot completedSlot = new ScheduleSlot();
        UUID completedSlotId = UUID.randomUUID();
        completedSlot.setId(completedSlotId);

        Appointment completedAppointment = new Appointment();
        completedAppointment.setId(completedAppointmentId);
        completedAppointment.setProfileId(profileId);
        completedAppointment.setTherapist(therapist);
        completedAppointment.setSlot(completedSlot);
        completedAppointment.setMode(AppointmentMode.VIDEO);
        completedAppointment.setStatus(AppointmentStatus.COMPLETED);
        completedAppointment.setStartDatetime(Instant.parse("2026-04-01T10:00:00Z"));

        ScheduleSlot cancelledSlot = new ScheduleSlot();
        UUID cancelledSlotId = UUID.randomUUID();
        cancelledSlot.setId(cancelledSlotId);

        Appointment cancelledAppointment = new Appointment();
        cancelledAppointment.setId(cancelledAppointmentId);
        cancelledAppointment.setProfileId(profileId);
        cancelledAppointment.setTherapist(therapist);
        cancelledAppointment.setSlot(cancelledSlot);
        cancelledAppointment.setMode(AppointmentMode.VIDEO);
        cancelledAppointment.setStatus(AppointmentStatus.CANCELLED);
        cancelledAppointment.setStartDatetime(Instant.parse("2026-03-25T10:00:00Z"));

        when(appointmentRepository.findByProfileIdAndStatusInOrderByStartDatetimeDesc(
            eq(profileId),
            eq(List.of(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED))
        )).thenReturn(List.of(completedAppointment, cancelledAppointment));

        List<AppointmentHistoryItemResponseDto> response =
            bookingService.getCompletedAndCancelledAppointments(profileId);

        assertEquals(2, response.size());

        AppointmentHistoryItemResponseDto firstItem = response.getFirst();
        assertEquals(completedAppointmentId, firstItem.appointmentId());
        assertEquals(profileId, firstItem.profileId());
        assertEquals(therapistId, firstItem.therapistId());
        assertEquals("Dr. Alice Carter", firstItem.therapistName());
        assertEquals("Anxiety", firstItem.therapistSpecialization());
        assertEquals("Canada", firstItem.location());
        assertEquals(completedSlotId, firstItem.slotId());
        assertEquals("VIDEO", firstItem.mode());
        assertEquals("COMPLETED", firstItem.status());
        assertEquals(Instant.parse("2026-04-01T10:00:00Z"), firstItem.startDatetime());

        AppointmentHistoryItemResponseDto secondItem = response.get(1);
        assertEquals(cancelledAppointmentId, secondItem.appointmentId());
        assertEquals(cancelledSlotId, secondItem.slotId());
        assertEquals("CANCELLED", secondItem.status());
        }

        @Test
        void getCompletedAndCancelledAppointments_returnsEmptyListWhenNoHistoryFound() {
        UUID profileId = UUID.randomUUID();
        when(appointmentRepository.findByProfileIdAndStatusInOrderByStartDatetimeDesc(
            eq(profileId),
            eq(List.of(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED))
        )).thenReturn(List.of());

        List<AppointmentHistoryItemResponseDto> response =
            bookingService.getCompletedAndCancelledAppointments(profileId);

        assertEquals(0, response.size());
        }
}
