package com.booking.therapist_api.service.impl;

import com.booking.therapist_api.service.VideoConsultationProvider;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class JitsiVideoServiceImpl implements VideoConsultationProvider {

    @Override
    public String createVideoRoom() {
        return "https://meet.jit.si/uMatter-Consult-" + UUID.randomUUID();
    }
}
