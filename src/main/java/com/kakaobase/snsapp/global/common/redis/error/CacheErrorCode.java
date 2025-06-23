package com.kakaobase.snsapp.global.common.redis.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CacheErrorCode{

    LOCK_ERROR("락 수행중 예외 발생"),
    LOCK_ACQUISITION_FAIL("락 수행중 예외 발생"),
    CREATE_CACHE_ERROR("캐시 생성중 에러 발생"),
    CACHE_ALREADY_EXISTS("이미 존재하는 캐시에 생성 시도"),
    SYNC_ERROR("캐시 동기화 중 에러 발생"),
    FIELD_UPDATE_ERROR("필드 업데이트 중 오류 발생");

    private final String message;
}
