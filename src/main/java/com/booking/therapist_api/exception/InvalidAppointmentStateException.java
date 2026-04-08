package com.booking.therapist_api.exception;

public class InvalidAppointmentStateException extends RuntimeException {

    public InvalidAppointmentStateException(String message) {
        super(message);
    }
}
