package com.kakaobase.snsapp.domain.notification.dto.records;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record NotificationData(
        Long id,
        MemberResponseDto.UserInfo sender,
        Long target_id,
        String content,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp
) {}
