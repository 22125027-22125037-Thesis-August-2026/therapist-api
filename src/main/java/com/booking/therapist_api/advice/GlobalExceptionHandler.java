package com.booking.therapist_api.advice;

import com.booking.therapist_api.exception.ResourceNotFoundException;
import com.booking.therapist_api.exception.MeetingNotOpenException;
import com.booking.therapist_api.exception.ClinicalNoteAlreadyExistsException;
import com.booking.therapist_api.exception.InvalidAppointmentStateException;
import com.booking.therapist_api.exception.SlotAlreadyBookedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(SlotAlreadyBookedException.class)
    public ProblemDetail handleSlotAlreadyBookedException(SlotAlreadyBookedException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Booking Conflict");
        return problemDetail;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Resource Not Found");
        return problemDetail;
    }

    @ExceptionHandler(MeetingNotOpenException.class)
    public ProblemDetail handleMeetingNotOpenException(MeetingNotOpenException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problemDetail.setTitle("Room Not Open");
        return problemDetail;
    }

    @ExceptionHandler(InvalidAppointmentStateException.class)
    public ProblemDetail handleInvalidAppointmentStateException(InvalidAppointmentStateException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Invalid Appointment State");
        return problemDetail;
    }

    @ExceptionHandler(ClinicalNoteAlreadyExistsException.class)
    public ProblemDetail handleClinicalNoteAlreadyExistsException(ClinicalNoteAlreadyExistsException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Clinical Note Conflict");
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed for one or more request fields"
        );
        problemDetail.setTitle("Validation Error");

        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> fieldErrors
                .computeIfAbsent(error.getField(), key -> new java.util.ArrayList<>())
                .add(error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value"));

        problemDetail.setProperty("errors", fieldErrors);
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception ex) {
        logger.error("Unhandled exception occurred", ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later."
        );
        problemDetail.setTitle("Internal Server Error");
        return problemDetail;
    }
}
