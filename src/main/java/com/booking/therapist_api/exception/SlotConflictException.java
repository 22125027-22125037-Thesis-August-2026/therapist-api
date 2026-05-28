package com.booking.therapist_api.exception;

public class SlotConflictException extends RuntimeException {
    public SlotConflictException(String message) {
        super(message);
    }
}
