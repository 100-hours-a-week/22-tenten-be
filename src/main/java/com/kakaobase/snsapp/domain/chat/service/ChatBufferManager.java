package com.kakaobase.snsapp.domain.chat.service;

import com.kakaobase.snsapp.domain.chat.dto.ai.request.ChatBlockData;
import com.kakaobase.snsapp.domain.chat.exception.ChatException;
import com.kakaobase.snsapp.domain.chat.exception.errorcode.ChatErrorCode;
import com.kakaobase.snsapp.domain.chat.util.ChatBufferCacheUtil;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 채팅 버퍼 → AI 서버 전송 처리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatBufferManager {
    
    private final ChatBufferCacheUtil cacheUtil;
    private final StreamingSessionManager streamingSessionManager;
    private final AiServerHttpClient aiServerHttpClient;
    private final ChatCommandService chatCommandService;
    private final MemberRepository memberRepository;
    private final AiServerSseManager aiServerSseManager;
    
    /**
     * 채팅 버퍼를 AI 서버로 전송 (타이머에서 호출)
     */
    public void sendBufferToAiServer(Long userId) {
        log.info("AI 서버 자동 전송 시작: userId={}", userId);
        
        // 1. AI 서버 상태 확인
        if (aiServerSseManager.getHealthStatus().isDisconnected()) {
            log.warn("AI 서버 연결 상태 불량으로 자동 전송 중단: userId={}, status={}", 
                userId, aiServerSseManager.getHealthStatus());
            return;
        }
        
        // 2. 버퍼 존재 여부 확인
        if (!cacheUtil.hasBuffer(userId)) {
            log.debug("버퍼가 없어서 전송 안함: userId={}", userId);
            return;
        }
        
        // 3. 버퍼 내용 가져오기 및 삭제
        String bufferContent = cacheUtil.getAndDeleteBuffer(userId);
        if (bufferContent == null || bufferContent.trim().isEmpty()) {
            log.debug("버퍼 내용이 비어서 전송 안함: userId={}", userId);
            return;
        }
        
        // 4. 사용자 정보 조회
        Member member = memberRepository.findById(userId)
            .orElseThrow(() -> new ChatException(ChatErrorCode.USER_NOT_FOUND, userId));
        
        // 5. 스트리밍 세션 시작 및 StreamId 생성
        String streamId = streamingSessionManager.startStreaming(userId);
        
        // 6. ChatBlockData 생성
        ChatBlockData chatBlockData = ChatBlockData.builder()
            .streamId(streamId)
            .nickname(member.getNickname())
            .className(member.getClassName())
            .content(bufferContent)
            .timestamp(LocalDateTime.now())
            .isRead(false)
            .build();
        
        // 7. AI 서버로 HTTP 전송
        aiServerHttpClient.sendChatBlock(chatBlockData);
        
        // 8. 사용자에게 로딩 알림
        chatCommandService.sendLoadingToUser(userId);
        
        log.info("AI 서버 자동 전송 완료: userId={}, streamId={}, contentLength={}", 
            userId, streamId, bufferContent.length());
    }
}