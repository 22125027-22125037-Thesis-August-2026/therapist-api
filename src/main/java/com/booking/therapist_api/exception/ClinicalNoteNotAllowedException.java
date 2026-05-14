package com.booking.therapist_api.exception;

public class ClinicalNoteNotAllowedException extends RuntimeException {

    public ClinicalNoteNotAllowedException(String message) {
        super(message);
    }
}
