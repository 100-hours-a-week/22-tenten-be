package com.kakaobase.snsapp.domain.chat.exception.errorcode;

import lombok.Getter;

/**
 * 스트리밍 중 발생하는 에러 모음
 * 모든 event는 chat.stream.error
 */
@Getter
public enum StreamErrorCode {
    
    // AI 서버 연결 관련 에러
    AI_SERVER_ERROR("ai_server_error", "AI 서버에 문제가 발생하였습니다"),
    AI_SERVER_TIMEOUT("ai_server_timeout", "AI 서버 응답 시간이 초과되었습니다"),
    
    // AI 서버 응답 관련 에러
    AI_SERVER_RESPONSE_INVALID("AI 서버 응답 형식이 올바르지 않습니다"),
    AI_SERVER_RESPONSE_PARSE_FAIL("AI 서버 응답 파싱에 실패했습니다"),
    
    // 메시지 전송 관련 에러
    AI_SERVER_MESSAGE_SEND_FAIL("AI 서버로 메시지 전송에 실패했습니다"),
    
    // 스트리밍 세션 관련 에러
    STREAM_SESSION_NOT_FOUND("스트리밍 세션을 찾을 수 없습니다"),
    INVALID_STREAM_ID("유효하지 않은 스트림 ID입니다"),
    STREAM_END_EVENT_FAIL("스트림 종료 패킷 처리에 실패하였습니다.");

    
    private final String event;
    private final String error;
    private final String message;

    StreamErrorCode(String message, String error) {
        this.event = "chat.stream.error";
        this.error = error;
        this.message = message;
    }

    StreamErrorCode(String message) {
        this.event = "chat.stream.error";
        this.error = "internal_server_error";
        this.message = message;
    }
}