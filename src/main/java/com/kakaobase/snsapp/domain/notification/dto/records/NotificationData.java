package com.kakaobase.snsapp.domain.notification.dto.records;

import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;

import java.time.LocalDateTime;

public record NotificationData(
        Long id,
        MemberResponseDto.UserInfo sender,
        Long target_id,
        String message,
        LocalDateTime timestamp
) {}
