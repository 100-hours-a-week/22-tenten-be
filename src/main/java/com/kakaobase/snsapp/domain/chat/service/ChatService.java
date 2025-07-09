package com.kakaobase.snsapp.domain.chat.service;

import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.chat.converter.ChatConverter;
import com.kakaobase.snsapp.domain.chat.dto.request.ChatData;
import com.kakaobase.snsapp.domain.chat.dto.response.ChatList;
import com.kakaobase.snsapp.domain.chat.entity.ChatMessage;
import com.kakaobase.snsapp.domain.chat.entity.ChatRoom;
import com.kakaobase.snsapp.domain.chat.repository.ChatMessageRepository;
import com.kakaobase.snsapp.domain.chat.repository.ChatRoomMemberRepository;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.global.common.constant.BotConstants;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatConverter chatConverter;
    private final ChatCommandService commandService;
    private final EntityManager em;

    // ====== 채팅 조회 관련 메서드들 ======

    /**
     * 봇과의 채팅 목록 조회 (기존 메서드)
     */
    @Transactional(readOnly = true)
    public ChatList getChatingWithBot(CustomUserDetails userDetails, Integer limit, Long cursor) {
        Long userId = Long.valueOf(userDetails.getId());
        log.info("봇과의 채팅 목록 조회: userId={}", userId);

        //기존 채팅방이 없다면 채팅방 생성후 빈 값 전송
        if(!chatRoomMemberRepository.existsByChatRoomIdAndMemberId(userId, BotConstants.BOT_MEMBER_ID)) {
            commandService.createBotChatRoom(userId);
            return new ChatList(
                    null,
                    false
            );
        }
        
        return chatMessageRepository.findMessagesByChatRoomId(userId, limit, cursor);
    }

    /**
     * 특정 채팅방의 메시지 목록 조회
     */
    @Transactional(readOnly = true)
    public ChatList getChatMessages(Long chatRoomId, Integer limit, Long cursor) {
        log.info("채팅 메시지 조회: chatRoomId={}, limit={}, cursor={}", chatRoomId, limit, cursor);
        
        return chatMessageRepository.findMessagesByChatRoomId(chatRoomId, limit, cursor);
    }

    // ====== 채팅 메시지 처리 관련 메서드들 ======

    /**
     * 사용자 채팅 메시지 저장
     */
    @Async
    @Transactional
    public CompletableFuture<Long> saveUserMessage(Long userId, ChatData chatData) {
        log.info("사용자 메시지 저장: userId={}, content={}", userId, chatData.content());
        
        try {
            ChatMessage chatMessage = commandService.saveChatMessage(userId, chatData.content());
            return CompletableFuture.completedFuture(chatMessage.getId());
            
        } catch (Exception e) {
            log.error("사용자 메시지 저장 실패: userId={}, error={}", userId, e.getMessage(), e);
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * AI 응답 메시지 저장
     */
    @Async
    @Transactional
    public CompletableFuture<Long> saveAiMessage(Long userId, String aiResponse, Long originalMessageId) {
        log.info("AI 응답 메시지 저장: userId={}, originalMessageId={}", userId, originalMessageId);
        
        try {

            //기본적으로 소셜봇과 유저의 ChatRoomId = userId
            ChatRoom chatRoom = em.getReference(ChatRoom.class, userId);
            Member botMember = em.getReference(Member.class, BotConstants.BOT_MEMBER_ID);
            
            // AI 응답 메시지 생성 및 저장
            ChatMessage aiMessage = chatConverter.createBotMessage(aiResponse, botMember, chatRoom);
            ChatMessage savedMessage = chatMessageRepository.save(aiMessage);
            
            return CompletableFuture.completedFuture(savedMessage.getId());
            
        } catch (Exception e) {
            log.error("AI 응답 메시지 저장 실패: userId={}, error={}", userId, e.getMessage(), e);
            return CompletableFuture.completedFuture(null);
        }
    }

    // ====== 메시지 읽음 상태 관리 ======

    /**
     * 메시지 읽음 처리 (소셜봇 채팅에서는 실시간이므로 별도 처리 불필요)
     */
    @Async
    @Transactional
    public void markMessagesAsRead(Long userId, Long chatRoomId, List<Long> messageIds) {
        log.info("메시지 읽음 처리: userId={}, chatRoomId={}, messageCount={}", userId, chatRoomId, messageIds.size());
        
        try {
            // 소셜봇 채팅에서는 실시간 대화이므로 읽음 처리 로직 생략
            log.info("메시지 읽음 처리 완료: userId={}, chatRoomId={}", userId, chatRoomId);
        } catch (Exception e) {
            log.error("메시지 읽음 처리 실패: userId={}, chatRoomId={}, error={}", userId, chatRoomId, e.getMessage(), e);
        }
    }
}
