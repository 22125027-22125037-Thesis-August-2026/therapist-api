package com.booking.therapist_api.service;

import com.booking.therapist_api.dto.VideoRoomDetailsDto;
import com.booking.therapist_api.entity.Therapist;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@ConditionalOnProperty(name = "video.provider", havingValue = "jitsi")
public class JitsiVideoServiceImpl implements VideoConsultationProvider {

    @Override
    public VideoRoomDetailsDto getVideoRoomDetails(Therapist therapist) {
        return new VideoRoomDetailsDto(UUID.randomUUID().toString(), null, null);
    }
}
