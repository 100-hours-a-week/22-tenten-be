package com.kakaobase.snsapp.domain.chat.repository.custom;

import com.kakaobase.snsapp.domain.chat.converter.ChatConverter;
import com.kakaobase.snsapp.domain.chat.dto.response.ChatItemDto;
import com.kakaobase.snsapp.domain.chat.dto.response.ChatList;
import com.kakaobase.snsapp.domain.chat.entity.ChatMessage;
import com.kakaobase.snsapp.domain.chat.entity.QChatMessage;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatMessageCustomRepositoryImpl implements ChatMessageCustomRepository {

    private final JPAQueryFactory queryFactory;
    private final ChatConverter chatConverter;

    @Override
    public ChatList findMessagesByChatRoomId(Long chatRoomId, Integer limit, Long cursor) {
        QChatMessage chatMessage = QChatMessage.chatMessage;

        // WHERE 조건 빌더
        BooleanBuilder whereClause = new BooleanBuilder();
        whereClause.and(chatMessage.chatRoom.id.eq(chatRoomId));
        
        if (cursor != null) {
            whereClause.and(chatMessage.id.gt(cursor));
        }

        // DB에서 최신순으로 limit + 1 개 조회 (hasNext 판단용)
        List<ChatMessage> messages = queryFactory
                .selectFrom(chatMessage)
                .where(whereClause)
                .orderBy(chatMessage.createdAt.desc())
                .limit((long) limit + 1)
                .fetch();

        // hasNext 계산
        boolean hasNext = messages.size() > limit;
        if (hasNext) {
            messages = messages.subList(0, limit);
        }

        // List 내에서 오래된 순으로 재정렬
        Collections.reverse(messages);

        // ChatMessage를 ChatItemDto로 변환
        List<ChatItemDto> chatItemDtoList = messages.stream()
                .map(chatConverter::toChatItemDto)
                .toList();

        return new ChatList(chatItemDtoList, hasNext);
    }
}