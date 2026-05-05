package com.booking.therapist_api.service;

import com.booking.therapist_api.dto.VideoRoomDetailsDto;
import com.booking.therapist_api.entity.Therapist;
import com.booking.therapist_api.entity.TherapistZoomCredential;
import com.booking.therapist_api.exception.ResourceNotFoundException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class ZoomVideoServiceImpl implements VideoConsultationProvider {

    @Value("${zoom.app-key}")
    private String appKey;

    @Value("${zoom.app-secret}")
    private String appSecret;

    @Override
    public VideoRoomDetailsDto getVideoRoomDetails(Therapist therapist) {
        TherapistZoomCredential credential = therapist.getZoomCredential();
        if (credential == null) {
            throw new ResourceNotFoundException(
                    "No Zoom credentials found for therapist id: " + therapist.getTherapistId());
        }
        return new VideoRoomDetailsDto(
                credential.getZoomMeetingNumber(),
                credential.getZoomMeetingPassword(),
                generateZoomSdkJwt()
        );
    }

    private String generateZoomSdkJwt() {
        long iat = Instant.now().getEpochSecond() - 30;
        long exp = iat + 7200;
        SecretKey key = Keys.hmacShaKeyFor(appSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .claim("appKey", appKey)
                .issuedAt(Date.from(Instant.ofEpochSecond(iat)))
                .expiration(Date.from(Instant.ofEpochSecond(exp)))
                .claim("tokenExp", exp)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}
