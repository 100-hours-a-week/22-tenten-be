package com.kakaobase.snsapp.domain.chat.dto.ai.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AI 서버 HTTP 응답 구조체
 */
public record AiServerResponse(
    @JsonProperty("message")
    String message,
    @JsonProperty("error")
    String error
) {
    /**
     * 성공 응답인지 확인
     */
    public boolean isSuccess() {
        return error == null || error.isBlank();
    }
    
    /**
     * 에러 응답인지 확인
     */
    public boolean isError() {
        return !isSuccess();
    }
    
    /**
     * StreamQueue 등록 성공 응답인지 확인
     */
    public boolean isStreamQueueRegistered() {
        return isSuccess() && message != null && message.contains("StreamQueue등록 완료");
    }
}