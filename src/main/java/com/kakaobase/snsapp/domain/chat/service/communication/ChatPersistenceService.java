package com.kakaobase.snsapp.domain.chat.service.communication;

import com.kakaobase.snsapp.domain.chat.converter.ChatConverter;
import com.kakaobase.snsapp.domain.chat.entity.ChatMessage;
import com.kakaobase.snsapp.domain.chat.entity.ChatRoom;
import com.kakaobase.snsapp.domain.chat.entity.ChatRoomMember;
import com.kakaobase.snsapp.domain.chat.exception.ChatException;
import com.kakaobase.snsapp.domain.chat.exception.errorcode.ChatErrorCode;
import com.kakaobase.snsapp.domain.chat.repository.ChatMessageRepository;
import com.kakaobase.snsapp.domain.chat.repository.ChatRoomMemberRepository;
import com.kakaobase.snsapp.domain.chat.repository.ChatRoomRepository;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.global.common.constant.BotConstants;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 채팅 데이터 저장 전용 서비스
 * 채팅 관련 데이터베이스 트랜잭션 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatPersistenceService {
    
    private final EntityManager em;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatConverter chatConverter;
    
    /**
     * 새 채팅방 생성
     */
    @Transactional
    public Long createBotChatRoom(Long userId) {
        log.info("새 채팅방 생성: userId={}", userId);
        
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
        
        log.info("채팅방 생성 완료: userId={}, chatRoomId={}", userId, savedChatRoom.getId());
        return savedChatRoom.getId();
    }
    
    /**
     * 채팅 메시지 저장
     */
    @Async
    @Transactional
    public void saveChatMessage(Long senderId, String content) {
        log.info("채팅 메시지 저장: senderId={}, contentLength={}", 
            senderId, content != null ? content.length() : 0);
        
        try {
            // 메시지 엔티티 생성 및 저장
            ChatMessage message = chatConverter.toChatMessage(senderId, content);
            ChatMessage savedMessage = chatMessageRepository.save(message);
            
            log.info("채팅 메시지 저장 완료: senderId={}, messageId={}", senderId, savedMessage.getId());
            
        } catch (Exception e) {
            log.error("채팅 메시지 저장 실패: senderId={}, error={}", senderId, e.getMessage(), e);
            throw new ChatException(ChatErrorCode.MESSAGE_SAVE_FAIL, senderId);
        }
    }
    
    /**
     * 채팅 메시지 저장 (동기 버전)
     */
    @Transactional
    public Long saveChatMessageSync(Long senderId, String content) {
        log.info("채팅 메시지 저장 (동기): senderId={}, contentLength={}", 
            senderId, content != null ? content.length() : 0);
        
        try {
            // 메시지 엔티티 생성 및 저장
            ChatMessage message = chatConverter.toChatMessage(senderId, content);
            ChatMessage savedMessage = chatMessageRepository.save(message);
            
            log.info("채팅 메시지 저장 완료: senderId={}, messageId={}", senderId, savedMessage.getId());
            return savedMessage.getId();
            
        } catch (Exception e) {
            log.error("채팅 메시지 저장 실패: senderId={}, error={}", senderId, e.getMessage(), e);
            throw new ChatException(ChatErrorCode.MESSAGE_SAVE_FAIL, senderId);
        }
    }
    
    /**
     * AI 응답 메시지 저장
     */
    @Async
    @Transactional
    public void saveBotMessage(Long userId, String botResponse) {
        log.info("AI 응답 메시지 저장: userId={}, responseLength={}", 
            userId, botResponse != null ? botResponse.length() : 0);
        
        try {
            // AI 응답 메시지 생성 및 저장
            ChatMessage botMessage = chatConverter.toBotMessage(userId, botResponse);
            ChatMessage savedMessage = chatMessageRepository.save(botMessage);
            
            log.info("AI 응답 메시지 저장 완료: userId={}, messageId={}", userId, savedMessage.getId());
            
        } catch (Exception e) {
            log.error("AI 응답 메시지 저장 실패: userId={}, error={}", userId, e.getMessage(), e);
            throw new ChatException(ChatErrorCode.MESSAGE_SAVE_FAIL, userId);
        }
    }
    
    /**
     * AI 응답 메시지 저장 (동기 버전)
     */
    @Transactional
    public Long saveBotMessageSync(Long userId, String botResponse) {
        log.info("AI 응답 메시지 저장 (동기): userId={}, responseLength={}", 
            userId, botResponse != null ? botResponse.length() : 0);
        
        try {
            // AI 응답 메시지 생성 및 저장
            ChatMessage botMessage = chatConverter.toBotMessage(userId, botResponse);
            ChatMessage savedMessage = chatMessageRepository.save(botMessage);
            
            log.info("AI 응답 메시지 저장 완료: userId={}, messageId={}", userId, savedMessage.getId());
            return savedMessage.getId();
            
        } catch (Exception e) {
            log.error("AI 응답 메시지 저장 실패: userId={}, error={}", userId, e.getMessage(), e);
            throw new ChatException(ChatErrorCode.MESSAGE_SAVE_FAIL, userId);
        }
    }
    
    /**
     * 메시지 상태 업데이트 (읽음, 삭제 등)
     */
    @Transactional
    public void updateMessageStatus(Long chatId, Long userId) {
        log.info("메시지 상태 업데이트: messageId={}, userId={}", chatId, userId);
        
        // TODO: 메시지 상태 읽음 업데이트 로직 구현
        // 현재는 로깅만 수행
    }
}
