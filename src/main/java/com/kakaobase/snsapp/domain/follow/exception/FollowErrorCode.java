package com.kakaobase.snsapp.domain.follow.exception;


import com.kakaobase.snsapp.global.error.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FollowErrorCode implements BaseErrorCode {

    ALREADY_FOLLOWING(HttpStatus.CONFLICT, "state_conflict", "이미 팔로잉한 유저입니다", "traget_user_id"),
    ALREADY_UNFOLLOWING(HttpStatus.CONFLICT, "state_conflict", "이미 언팔로잉한 유저입니다", "traget_user_id");

    private final HttpStatus status;
    private final String error;
    private final String message;
    private final String field;
}
