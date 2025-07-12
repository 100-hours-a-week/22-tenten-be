package com.kakaobase.snsapp.fixture.chat;

import com.kakaobase.snsapp.domain.chat.dto.SimpTimeData;
import com.kakaobase.snsapp.domain.chat.dto.ai.request.ChatBlockData;
import com.kakaobase.snsapp.domain.chat.dto.ai.response.AiStreamData;
import com.kakaobase.snsapp.domain.chat.dto.request.ChatData;
import com.kakaobase.snsapp.domain.chat.dto.request.StreamStopData;
import com.kakaobase.snsapp.domain.chat.dto.response.ChatItemDto;
import com.kakaobase.snsapp.domain.chat.dto.response.ChatList;
import com.kakaobase.snsapp.domain.chat.entity.ChatMessage;
import com.kakaobase.snsapp.domain.chat.entity.ChatRoom;
import com.kakaobase.snsapp.domain.chat.entity.ChatRoomMember;
import com.kakaobase.snsapp.domain.chat.event.ChatErrorEvent;
import com.kakaobase.snsapp.domain.chat.event.LoadingEvent;
import com.kakaobase.snsapp.domain.chat.event.StreamStartEvent;
import com.kakaobase.snsapp.domain.chat.exception.errorcode.ChatErrorCode;
import com.kakaobase.snsapp.domain.chat.model.StreamingSession;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.fixture.common.AbstractFixture;
import com.kakaobase.snsapp.fixture.members.MemberFixture;
import com.kakaobase.snsapp.global.common.constant.BotConstants;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Chat 도메인 테스트 데이터 생성을 위한 Fixture 클래스
 *
 * 제공 기능:
 * - Chat 엔티티 객체 생성 (ChatMessage, ChatRoom, ChatRoomMember)
 * - 스트리밍 시나리오 생성 (청크 기반 메시지 처리)
 * - DTO 및 이벤트 객체 생성
 * - 복합 채팅 시나리오 생성
 *
 * 주요 특징:
 * - MemberFixture와 연동하여 실제 Member 엔티티 사용
 * - 스트리밍 메시지를 청크 배열로 관리
 * - 봇 관련 상수 활용 (BotConstants)
 */
public class ChatFixture extends AbstractFixture {
    
    // === 봇 관련 상수 활용 ===
    public static final Long BOT_MEMBER_ID = BotConstants.BOT_MEMBER_ID;
    public static final String BOT_NICKNAME = BotConstants.BOT_NICKNAME;
    
    // === 기본 테스트 데이터 상수 ===
    private static final String DEFAULT_USER_MESSAGE = "안녕하세요! 테스트 메시지입니다.";
    private static final String DEFAULT_STREAM_ID_PREFIX = "test-stream-";
    
    // === 스트리밍 봇 메시지 상수 (청크 배열) ===
    private static final String[] DEFAULT_BOT_MESSAGE_CHUNKS = {
        "안녕하세요! ",
        "AI 봇 로로입니다. ",
        "무엇을 도와드릴까요? ",
        "궁금한 점이 있으시면 ",
        "언제든지 말씀해주세요!"
    };
    
    private static final String[] LONG_BOT_MESSAGE_CHUNKS = {
        "안녕하세요! 저는 AI 봇 로로입니다. ",
        "오늘은 어떤 하루를 보내고 계신가요? ",
        "혹시 궁금한 점이나 도움이 필요한 일이 있으시면 ",
        "언제든지 편하게 말씀해주세요. ",
        "함께 문제를 해결해보아요! ",
        "좋은 하루 되세요! 😊"
    };
    
    private static final String[] ERROR_BOT_MESSAGE_CHUNKS = {
        "죄송합니다. ",
        "일시적인 오류가 발생했습니다. ",
        "잠시 후 다시 시도해주세요."
    };
    
    // === Member 필요한 엔티티 생성 메서드 ===
    
    /**
     * 기본 사용자 메시지 생성 (MemberFixture 활용)
     */
    public static ChatMessage createDefaultUserMessage() {
        Member member = MemberFixture.createDefaultMember();
        setId(member, generateRandomId());
        ChatRoom chatRoom = createBotChatRoom(member.getId());
        return createUserMessage(member, chatRoom, DEFAULT_USER_MESSAGE);
    }
    
    /**
     * 닉네임 기반 사용자 메시지 생성
     */
    public static ChatMessage createUserMessageWithNickname(String nickname) {
        Member member = MemberFixture.createMemberWithNickname(nickname);
        setId(member, generateRandomId());
        ChatRoom chatRoom = createBotChatRoom(member.getId());
        return createUserMessage(member, chatRoom, DEFAULT_USER_MESSAGE);
    }
    
    /**
     * 사용자 메시지 생성 (Member 엔티티 활용)
     */
    public static ChatMessage createUserMessage(Member member, ChatRoom chatRoom, String content) {
        ChatMessage message = ChatMessage.builder()
            .member(member)
            .chatRoom(chatRoom)
            .content(content)
            .isRead(false)
            .build();
        setId(message, generateRandomId());
        setCreatedAt(message, now());
        return message;
    }
    
    /**
     * 봇 메시지 생성 (봇 Member 엔티티 활용)
     */
    public static ChatMessage createBotMessage(ChatRoom chatRoom, String content) {
        Member botMember = createBotMember();
        ChatMessage message = ChatMessage.builder()
            .member(botMember)
            .chatRoom(chatRoom)
            .content(content)
            .isRead(false)
            .build();
        setId(message, generateRandomId());
        setCreatedAt(message, now());
        return message;
    }
    
    /**
     * 봇 채팅방 생성 
     */
    public static ChatRoom createBotChatRoom(Long userId) {
        ChatRoom chatRoom = ChatRoom.builder()
            .id(userId) // 채팅방 ID = 사용자 ID
            .build();
        setCreatedAt(chatRoom, now());
        return chatRoom;
    }
    
    /**
     * 채팅방 멤버 생성 (사용자 + 봇)
     */
    public static List<ChatRoomMember> createBotChatRoomMembers(Member userMember, ChatRoom chatRoom) {
        Member botMember = createBotMember();
        return List.of(
            new ChatRoomMember(userMember, chatRoom),
            new ChatRoomMember(botMember, chatRoom)
        );
    }
    
    /**
     * 테스트용 봇 Member 생성
     */
    public static Member createBotMember() {
        Member botMember = Member.builder()
            .email("bot@kakaobase.com")
            .name("로로봇")
            .nickname(BOT_NICKNAME)
            .password("botpassword")
            .className(Member.ClassName.PANGYO_2)
            .build();
        setId(botMember, BOT_MEMBER_ID);
        return botMember;
    }
    
    // === 스트리밍 시나리오 생성 메서드 ===
    
    /**
     * 기본 스트리밍 시나리오 생성
     */
    public static StreamingScenario createDefaultStreamingScenario(Long userId) {
        String streamId = generateTestStreamId();
        StreamingSession session = new StreamingSession(userId);
        List<AiStreamData> chunks = createStreamingChunks(streamId, DEFAULT_BOT_MESSAGE_CHUNKS);
        
        return new StreamingScenario(streamId, session, chunks);
    }
    
    /**
     * 긴 메시지 스트리밍 시나리오 생성
     */
    public static StreamingScenario createLongStreamingScenario(Long userId) {
        String streamId = generateTestStreamId();
        StreamingSession session = new StreamingSession(userId);
        List<AiStreamData> chunks = createStreamingChunks(streamId, LONG_BOT_MESSAGE_CHUNKS);
        
        return new StreamingScenario(streamId, session, chunks);
    }
    
    /**
     * 에러 메시지 스트리밍 시나리오 생성
     */
    public static StreamingScenario createErrorStreamingScenario(Long userId) {
        String streamId = generateTestStreamId();
        StreamingSession session = new StreamingSession(userId);
        List<AiStreamData> chunks = createStreamingChunks(streamId, ERROR_BOT_MESSAGE_CHUNKS);
        
        return new StreamingScenario(streamId, session, chunks);
    }
    
    /**
     * 커스텀 스트리밍 시나리오 생성
     */
    public static StreamingScenario createCustomStreamingScenario(Long userId, String[] messageChunks) {
        String streamId = generateTestStreamId();
        StreamingSession session = new StreamingSession(userId);
        List<AiStreamData> chunks = createStreamingChunks(streamId, messageChunks);
        
        return new StreamingScenario(streamId, session, chunks);
    }
    
    /**
     * 스트리밍 청크 데이터 생성
     */
    public static List<AiStreamData> createStreamingChunks(String streamId, String[] messageChunks) {
        List<AiStreamData> chunks = new ArrayList<>();
        LocalDateTime baseTime = now();
        
        for (int i = 0; i < messageChunks.length; i++) {
            AiStreamData chunk = AiStreamData.builder()
                .streamId(streamId)
                .message(messageChunks[i])
                .timestamp(baseTime.plusSeconds(i)) // 1초 간격
                .build();
            chunks.add(chunk);
        }
        
        return chunks;
    }
    
    /**
     * 스트림 데이터 생성 (개별 메시지)
     */
    public static AiStreamData createStreamData(String streamId, String message) {
        return AiStreamData.builder()
            .streamId(streamId)
            .message(message)
            .timestamp(now())
            .build();
    }
    
    /**
     * 스트리밍 완료 데이터 생성
     */
    public static AiStreamData createStreamCompleteData(String streamId) {
        return AiStreamData.builder()
            .streamId(streamId)
            .message(null) // 완료 시에는 메시지 없음
            .timestamp(now())
            .build();
    }
    
    /**
     * 스트리밍 에러 데이터 생성
     */
    public static AiStreamData createStreamErrorData(String streamId, String errorMessage) {
        return AiStreamData.builder()
            .streamId(streamId)
            .message(errorMessage)
            .timestamp(now())
            .build();
    }
    
    // === 복합 시나리오 생성 메서드 ===
    
    /**
     * 완전한 채팅 시나리오 생성 (사용자 + 봇 + 채팅방)
     */
    public static ChatScenario createCompleteChatScenario(String userNickname) {
        // 1. 사용자 생성
        Member userMember = MemberFixture.createMemberWithNickname(userNickname);
        setId(userMember, generateRandomId());
        
        // 2. 봇 채팅방 생성
        ChatRoom chatRoom = createBotChatRoom(userMember.getId());
        
        // 3. 채팅방 멤버 생성
        List<ChatRoomMember> members = createBotChatRoomMembers(userMember, chatRoom);
        
        // 4. 채팅 히스토리 생성
        List<ChatMessage> messages = createChatHistory(userMember, chatRoom, 3);
        
        return new ChatScenario(userMember, chatRoom, members, messages);
    }
    
    /**
     * 채팅 히스토리 생성 (사용자 메시지 + 봇 응답)
     */
    public static List<ChatMessage> createChatHistory(Member userMember, ChatRoom chatRoom, int messageCount) {
        List<ChatMessage> messages = new ArrayList<>();
        
        for (int i = 1; i <= messageCount; i++) {
            // 사용자 메시지
            ChatMessage userMessage = createUserMessage(userMember, chatRoom, "사용자 메시지 " + i);
            setId(userMessage, generateRandomId());
            setCreatedAt(userMessage, minutesAgo(messageCount - i + 1));
            messages.add(userMessage);
            
            // 봇 응답 메시지
            ChatMessage botMessage = createBotMessage(chatRoom, "봇 응답 메시지 " + i);
            setId(botMessage, generateRandomId());
            setCreatedAt(botMessage, minutesAgo(messageCount - i));
            messages.add(botMessage);
        }
        
        return messages;
    }
    
    // === DTO 및 기타 객체 생성 메서드 ===
    
    /**
     * 채팅 블록 데이터 생성
     */
    public static ChatBlockData createChatBlockData(String streamId, String message) {
        return ChatBlockData.builder()
            .streamId(streamId)
            .nickname("testuser")
            .className("PANGYO_1")
            .content(message)
            .timestamp(now())
            .isRead(false)
            .build();
    }
    
    /**
     * 채팅 데이터 생성
     */
    public static ChatData createChatData(String content) {
        return new ChatData(content, now());
    }
    
    /**
     * 스트림 중지 데이터 생성
     */
    public static StreamStopData createStreamStopData(String streamId) {
        return new StreamStopData(streamId, now());
    }
    
    /**
     * 시간 데이터 생성
     */
    public static SimpTimeData createSimpTimeData() {
        return new SimpTimeData(now());
    }
    
    /**
     * ChatMessage를 ChatItemDto로 변환
     */
    public static ChatItemDto toChatItemDto(ChatMessage message) {
        return ChatItemDto.builder()
            .chatId(message.getId())
            .senderId(message.getMember().getId())
            .content(message.getContent())
            .timestamp(message.getCreatedAt())
            .isRead(message.getIsRead())
            .build();
    }
    
    /**
     * ChatMessage 리스트를 ChatItemDto 리스트로 변환
     */
    public static List<ChatItemDto> toChatItemDtoList(List<ChatMessage> messages) {
        return messages.stream()
            .map(ChatFixture::toChatItemDto)
            .collect(Collectors.toList());
    }
    
    /**
     * ChatList 생성 (ChatMessage 리스트로부터)
     */
    public static ChatList createChatList(List<ChatMessage> messages, boolean hasNext) {
        List<ChatItemDto> chatItems = toChatItemDtoList(messages);
        return new ChatList(chatItems, hasNext);
    }
    
    /**
     * 스트리밍 세션 생성
     */
    public static StreamingSession createStreamingSession(Long userId) {
        return new StreamingSession(userId);
    }
    
    /**
     * 테스트용 스트림 ID 생성
     */
    public static String generateTestStreamId() {
        // Use same logic as generateRandomString to avoid circular dependency
        final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        java.util.Random random = new java.util.Random(12345L);
        String randomPart = random.ints(8, 0, ALPHANUMERIC.length())
                .mapToObj(ALPHANUMERIC::charAt)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
        return DEFAULT_STREAM_ID_PREFIX + randomPart;
    }
    
    /**
     * 공개 랜덤 문자열 생성 메서드 (테스트에서 사용)
     * 
     * @param length 생성할 문자열 길이
     * @return 랜덤 영숫자 문자열
     */
    public static String generateRandomString(int length) {
        // Use same logic as AbstractFixture.generateRandomString
        final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        java.util.Random random = new java.util.Random(12345L);
        return random.ints(length, 0, ALPHANUMERIC.length())
                .mapToObj(ALPHANUMERIC::charAt)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }
    
    // === 이벤트 객체 생성 메서드 ===
    
    /**
     * 채팅 에러 이벤트 생성
     */
    public static ChatErrorEvent createChatErrorEvent(Long userId, ChatErrorCode errorCode) {
        return new ChatErrorEvent(userId, errorCode, "테스트 에러 메시지", generateTestStreamId());
    }
    
    /**
     * 로딩 이벤트 생성
     */
    public static LoadingEvent createLoadingEvent(Long userId, String streamId) {
        return new LoadingEvent(userId, streamId, "테스트 메시지 내용");
    }
    
    /**
     * 스트림 시작 이벤트 생성
     */
    public static StreamStartEvent createStreamStartEvent(Long userId, String streamId) {
        return new StreamStartEvent(userId, streamId, "첫 번째 스트림 데이터");
    }
    
    // === 스트리밍 시뮬레이션 도우미 메서드 ===
    
    /**
     * 스트리밍 세션에 청크들을 순차적으로 추가
     */
    public static void simulateStreamingProcess(StreamingSession session, List<AiStreamData> chunks) {
        for (AiStreamData chunk : chunks) {
            if (chunk.message() != null) {
                session.appendResponse(chunk.message());
            }
        }
    }
    
    /**
     * 청크 배열에서 최종 메시지 생성
     */
    public static String getFinalMessageFromChunks(String[] chunks) {
        return String.join("", chunks);
    }
    
    /**
     * 청크 배열에서 최종 메시지 생성 (AiStreamData 리스트)
     */
    public static String getFinalMessageFromStreamData(List<AiStreamData> chunks) {
        return chunks.stream()
            .map(AiStreamData::message)
            .filter(Objects::nonNull)
            .collect(Collectors.joining(""));
    }
    
    // === AbstractFixture 구현 ===
    
    @Override
    protected Object reset() {
        // ChatFixture는 정적 메서드만 사용하므로 리셋할 상태가 없음
        return this;
    }
    
    // === 스트리밍 테스트 시나리오 래퍼 클래스 ===
    
    /**
     * 스트리밍 테스트 시나리오를 담는 래퍼 클래스
     */
    public static class StreamingScenario {
        private final String streamId;
        private final StreamingSession session;
        private final List<AiStreamData> chunks;
        private final String expectedFinalMessage;
        
        public StreamingScenario(String streamId, StreamingSession session, List<AiStreamData> chunks) {
            this.streamId = streamId;
            this.session = session;
            this.chunks = chunks;
            this.expectedFinalMessage = getFinalMessageFromStreamData(chunks);
        }
        
        public String getStreamId() { return streamId; }
        public StreamingSession getSession() { return session; }
        public List<AiStreamData> getChunks() { return chunks; }
        public String getExpectedFinalMessage() { return expectedFinalMessage; }
        
        public AiStreamData getFirstChunk() { return chunks.get(0); }
        public AiStreamData getLastChunk() { return chunks.get(chunks.size() - 1); }
        public int getChunkCount() { return chunks.size(); }
        
        /**
         * 스트리밍 프로세스 시뮬레이션
         */
        public void simulateStreaming() {
            simulateStreamingProcess(session, chunks);
        }
        
        /**
         * 특정 인덱스까지 스트리밍 시뮬레이션
         */
        public void simulateStreamingUntil(int index) {
            List<AiStreamData> partialChunks = chunks.subList(0, Math.min(index + 1, chunks.size()));
            simulateStreamingProcess(session, partialChunks);
        }
    }
    
    // === 완전한 채팅 시나리오 래퍼 클래스 ===
    
    /**
     * 완전한 채팅 시나리오 데이터를 담는 래퍼 클래스
     */
    public static class ChatScenario {
        private final Member userMember;
        private final ChatRoom chatRoom;
        private final List<ChatRoomMember> members;
        private final List<ChatMessage> messages;
        
        public ChatScenario(Member userMember, ChatRoom chatRoom, 
                          List<ChatRoomMember> members, List<ChatMessage> messages) {
            this.userMember = userMember;
            this.chatRoom = chatRoom;
            this.members = members;
            this.messages = messages;
        }
        
        public Member getUserMember() { return userMember; }
        public ChatRoom getChatRoom() { return chatRoom; }
        public List<ChatRoomMember> getMembers() { return members; }
        public List<ChatMessage> getMessages() { return messages; }
        
        public Long getUserId() { return userMember.getId(); }
        public Long getChatRoomId() { return chatRoom.getId(); }
        public int getMessageCount() { return messages.size(); }
        
        /**
         * 사용자 메시지들만 필터링
         */
        public List<ChatMessage> getUserMessages() {
            return messages.stream()
                .filter(message -> !message.getMember().getId().equals(BOT_MEMBER_ID))
                .collect(Collectors.toList());
        }
        
        /**
         * 봇 메시지들만 필터링
         */
        public List<ChatMessage> getBotMessages() {
            return messages.stream()
                .filter(message -> message.getMember().getId().equals(BOT_MEMBER_ID))
                .collect(Collectors.toList());
        }
    }
}