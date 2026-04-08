package com.booking.therapist_api.exception;

public class ClinicalNoteAlreadyExistsException extends RuntimeException {

    public ClinicalNoteAlreadyExistsException(String message) {
        super(message);
    }
}
