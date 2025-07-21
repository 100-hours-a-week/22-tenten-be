package com.kakaobase.snsapp.domain.chat.util;

/**
 * AI 서버 헬스체크 상태
 */
public enum AiServerHealthStatus {
    DISCONNECTED,  // 서버 부팅 시 초기 상태 또는 연결 실패
    CONNECTING,    // SSE 연결 시도 중
    CONNECTED;     // SSE 연결 성공 및 정상 동작
    
    /**
     * 연결이 끊어진 상태인지 확인 (DISCONNECTED 또는 CONNECTING)
     */
    public boolean isDisconnected() {
        return this == DISCONNECTED || this == CONNECTING;
    }
    
    /**
     * 정상 연결 상태인지 확인
     */
    public boolean isConnected() {
        return this == CONNECTED;
    }
}