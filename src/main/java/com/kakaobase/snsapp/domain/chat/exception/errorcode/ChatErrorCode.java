package com.kakaobase.snsapp.domain.chat.exception.errorcode;

import lombok.Getter;

/**
 * 유저와의 채팅중 발생하는 에러 모음
 * 모든 event는 chat.error로 통일
 */
@Getter
public enum ChatErrorCode {

    AI_SERVER_CONNECTION_FAIL("ai_server_error", "AI 서버 연결에 실패했습니다"),
    AI_SERVER_TIMEOUT("ai_server_timeout", "AI 서버 응답 시간이 초과되었습니다"),
    AI_SERVER_CLIENT_ERROR("ai_server_client_error", "잘못된 요청입니다"),
    AI_SERVER_INTERNAL_ERROR("ai_server_internal_error", "AI 서버 내부 오류가 발생했습니다"),

    // 채팅방 관련 에러
    CHAT_ROOM_NOT_FOUND("chat_room_not_found", "채팅방을 찾을 수 없습니다"),
    CHAT_ROOM_CREATE_FAIL("채팅방 생성에 실패했습니다"),
    
    // 메시지 관련 에러
    MESSAGE_SAVE_FAIL("메시지 저장에 실패했습니다"),
    CHAT_INVALID("invalid_format", "채팅 형식이 올바르지 않습니다."),
    CHAT_NOT_FOUND("chat_not_found", "채팅 형식이 올바르지 않습니다."),
    
    // 사용자 관련 에러
    USER_NOT_FOUND("resource_not_found", "사용자를 찾을 수 없습니다"),

    CHAT_BUFFER_EXTEND_FAIL( "버퍼의 유효기간을 늘리는데 실패했습니다."),

    // 타이핑 세션 버퍼 관련 에러
    CHAT_BUFFER_ADD_FAIL("버퍼에 메세지를 추가하는데 실패했습니다."),

    // 타이핑 메시지 관련 에러
    TYPING_MESSAGE_ADD_FAIL("타이핑 메시지 추가에 실패했습니다"),
    TYPING_MESSAGE_INVALID("typing_message_invalid", "타이핑 메시지가 올바르지 않습니다"),
    TYPING_MESSAGE_EMPTY("typing_message_empty", "타이핑 메시지가 비어있습니다");


    private final String event;
    private final String error;
    private final String message;

    ChatErrorCode(String message) {
        this.event = "chat.error";
        this.error = "internal_server_error";
        this.message = message;
    }

    ChatErrorCode(String error, String message) {
        this.event = "chat.stream.error";
        this.error = error;
        this.message = message;
    }
}