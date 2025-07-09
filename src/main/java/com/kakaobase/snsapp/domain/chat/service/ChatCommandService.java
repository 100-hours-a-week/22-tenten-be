package com.kakaobase.snsapp.domain.chat.service;

import com.kakaobase.snsapp.domain.chat.converter.ChatConverter;
import com.kakaobase.snsapp.domain.chat.dto.ai.request.AiChatRequest;
import com.kakaobase.snsapp.domain.chat.dto.ai.response.AiStreamData;
import com.kakaobase.snsapp.domain.chat.dto.request.ChatData;
import com.kakaobase.snsapp.domain.chat.entity.ChatMessage;
import com.kakaobase.snsapp.domain.chat.entity.ChatRoom;
import com.kakaobase.snsapp.domain.chat.entity.ChatRoomMember;
import com.kakaobase.snsapp.domain.chat.repository.ChatMessageRepository;
import com.kakaobase.snsapp.domain.chat.repository.ChatRoomMemberRepository;
import com.kakaobase.snsapp.domain.chat.repository.ChatRoomRepository;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import com.kakaobase.snsapp.global.common.constant.BotConstants;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatCommandService {

    private final EntityManager em;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatConverter chatConverter;
    
    @Qualifier("webFluxClient")
    private final WebClient webClient;
    
    @Value("${ai.server.chat.endpoint:/api/ai/chat}")
    private String aiChatEndpoint;

    // ====== 외부 API 호출 관련 메서드들 ======

    /**
     * AI 서버로 채팅 메시지 전송 (SSE 스트리밍)
     */
    @Async
    public CompletableFuture<Flux<AiStreamData>> sendMessageToAiServer(Long userId, String message) {
        log.info("AI 서버로 메시지 전송: userId={}, message={}", userId, message);
        
        try {
            AiChatRequest request = AiChatRequest.builder()
                    .userId(userId)
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .build();
            
            Flux<AiStreamData> streamFlux = webClient
                    .post()
                    .uri(aiChatEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .map(this::parseStreamData)
                    .timeout(Duration.ofSeconds(BotConstants.AI_SERVER_TIMEOUT_SECONDS))
                    .doOnNext(data -> log.debug("AI 스트림 데이터 수신: {}", data))
                    .doOnError(error -> log.error("AI 서버 통신 오류: {}", error.getMessage()))
                    .doOnComplete(() -> log.info("AI 스트림 완료: userId={}", userId));
            
            return CompletableFuture.completedFuture(streamFlux);
            
        } catch (Exception e) {
            log.error("AI 서버 메시지 전송 실패: userId={}, error={}", userId, e.getMessage(), e);
            return CompletableFuture.completedFuture(Flux.empty());
        }
    }

    /**
     * AI 서버 연결 상태 확인
     */
    @Async
    public CompletableFuture<Boolean> checkAiServerHealth() {
        log.info("AI 서버 상태 확인 중...");
        
        try {
            String response = webClient
                    .get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            log.info("AI 서버 상태 확인 성공: {}", response);
            return CompletableFuture.completedFuture(true);
            
        } catch (Exception e) {
            log.error("AI 서버 상태 확인 실패: {}", e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * AI 서버로부터 받은 스트림 데이터 처리
     */
    @Async
    public void processAiStreamData(Long userId, AiStreamData streamData) {
        log.info("AI 스트림 데이터 처리: userId={}, data={}", userId, streamData);
        
        try {
            // TODO: WebSocket으로 클라이언트에 스트림 데이터 전송
            // 스트림 완료 시 전체 메시지 저장 로직 추가 예정
            
        } catch (Exception e) {
            log.error("AI 스트림 데이터 처리 실패: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * SSE 스트림 데이터 파싱
     */
    private AiStreamData parseStreamData(String rawData) {
        try {
            // SSE 데이터 형식: "data: {json}"
            if (rawData.startsWith("data: ")) {
                String jsonData = rawData.substring(6);
                
                // 스트림 완료 신호
                if (jsonData.trim().equals("[DONE]")) {
                    return AiStreamData.builder()
                            .message("")
                            .isComplete(true)
                            .timestamp(LocalDateTime.now())
                            .build();
                }
                
                // 실제 메시지 데이터 파싱 (간단한 처리)
                return AiStreamData.builder()
                        .message(jsonData)
                        .isComplete(false)
                        .timestamp(LocalDateTime.now())
                        .build();
            }
            
            return AiStreamData.builder()
                    .message(rawData)
                    .isComplete(false)
                    .timestamp(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("스트림 데이터 파싱 실패: rawData={}, error={}", rawData, e.getMessage());
            return AiStreamData.builder()
                    .message("데이터 파싱 오류")
                    .isComplete(false)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    // ====== 데이터베이스 트랜잭션 관련 메서드들 ======

    /**
     * 새 채팅방 생성
     */
    @Transactional
    public Long createBotChatRoom(Long userId) {

        // 새로운 채팅방 생성
        ChatRoom newChatRoom = new ChatRoom(userId);
        ChatRoom savedChatRoom = chatRoomRepository.save(newChatRoom);

        // 사용자와 봇을 채팅방 멤버로 추가
        Member proxyUser = em.getReference(Member.class, userId);
        Member proxyBot = em.getReference(Member.class, BotConstants.BOT_MEMBER_ID);

        // 채팅방 멤버 추가
        ChatRoomMember userMember = new ChatRoomMember(proxyUser, savedChatRoom);
        ChatRoomMember botRoomMember = new ChatRoomMember(proxyBot, savedChatRoom);

        chatRoomMemberRepository.save(userMember);
        chatRoomMemberRepository.save(botRoomMember);

        return savedChatRoom.getId();
    }

    /**
     * 채팅 메시지 저장
     */
    @Transactional
    public ChatMessage saveChatMessage(Long senderId, String content) {
        // 사용자 조회
        Member proxyUser = em.getReference(Member.class, senderId);

        // 봇과의 채팅방 조회 또는 생성
        ChatRoom chatRoom = em.getReference(ChatRoom.class, senderId);

        // 메시지 엔티티 생성 및 저장
        ChatMessage message = chatConverter.toChatMessage(content, proxyUser, chatRoom);
        ChatMessage savedMessage = chatMessageRepository.save(message);
        return savedMessage;
    }

    /**
     * 대량 메시지 저장 (배치 처리)
     */
    @Transactional
    public List<ChatMessage> saveChatMessagesBatch(List<ChatMessage> messages) {
        // TODO: 대량 메시지 배치 저장 로직
        // - JPA 배치 처리로 성능 최적화
        // - 중복 검사 및 유효성 검증
        // - 저장 실패 시 롤백 처리
        log.info("대량 메시지 저장: messageCount={}", messages.size());
        return null;
    }

    /**
     * 메시지 상태 업데이트 (읽음, 삭제 등)
     */
    @Transactional
    public void updateMessageStatus(Long chatId, Long userId) {
        // TODO: 메시지 상태 업데이트 로직
        // - 메시지 읽음 상태 변경
        // - 삭제 상태 변경 (소프트 삭제)
        // - 상태 변경 시간 기록
        // - 권한 확인 (본인 메시지 또는 채팅방 관리자)
        log.info("메시지 상태 업데이트: messageId={}, userId={}", chatId, userId);
    }

    // ====== 복합 트랜잭션 처리 ======

    /**
     * 사용자 메시지 처리 (저장 + AI 서버 호출)
     */
    @Transactional
    public CompletableFuture<Long> processUserMessage(Long userId, ChatData chatData) {
        // TODO: 사용자 메시지 전체 처리 로직
        // - 1. 사용자 메시지를 DB에 저장
        // - 2. AI 서버로 메시지 전송 요청
        // - 3. 메시지 상태를 '전송 중'으로 업데이트
        // - 4. 알림 서비스에 메시지 전송 알림
        // - 트랜잭션 성공 시에만 AI 서버 호출
        log.info("사용자 메시지 처리: userId={}, content={}", userId, chatData.content());
        return CompletableFuture.completedFuture(null);
    }

    /**
     * AI 응답 완료 처리 (저장 + 상태 업데이트)
     */
    @Transactional
    public void processAiResponseComplete(Long userId, String aiResponse, Long originalMessageId) {
        // TODO: AI 응답 완료 처리 로직
        // - 1. AI 응답 메시지를 DB에 저장
        // - 2. 원본 메시지 상태를 '완료'로 업데이트
        // - 3. 채팅방 마지막 메시지 정보 업데이트
        // - 4. 알림 서비스에 응답 완료 알림
        // - 실패 시 모든 변경사항 롤백
        log.info("AI 응답 완료 처리: userId={}, originalMessageId={}", userId, originalMessageId);
    }

    /**
     * 채팅 세션 초기화 (채팅방 생성 + 멤버 추가)
     */
    @Transactional
    public ChatRoom initializeChatSession(Long userId) {
        // TODO: 채팅 세션 초기화 로직
        // - 1. 기존 봇 채팅방 확인
        // - 2. 없으면 새 채팅방 생성
        // - 3. 사용자와 봇을 멤버로 추가
        // - 4. 초기 환영 메시지 생성
        // - 모든 작업이 성공해야 세션 활성화
        log.info("채팅 세션 초기화: userId={}", userId);
        return null;
    }
}
