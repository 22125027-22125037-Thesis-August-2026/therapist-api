package com.booking.therapist_api.service;

import com.booking.therapist_api.dto.VideoRoomDetailsDto;
import com.booking.therapist_api.entity.Therapist;

public interface VideoConsultationProvider {
    VideoRoomDetailsDto getVideoRoomDetails(Therapist therapist);
}
