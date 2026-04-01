package com.booking.therapist_api.exception;

public class MeetingNotOpenException extends RuntimeException {

    public MeetingNotOpenException(String message) {
        super(message);
    }
}
