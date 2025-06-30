package com.kakaobase.snsapp.domain.posts.event;

import com.kakaobase.snsapp.domain.posts.util.BoardType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * 게시글 생성 이벤트
 *
 * <p>게시글이 생성될 때 발행되는 이벤트입니다.
 * 모든 게시글(봇 포함)이 5개 생성될 때마다 봇 게시글을 추가로 생성합니다.</p>
 */
@Getter
public class PostCreatedEvent extends ApplicationEvent {

    /**
     * 생성된 게시글 ID
     */
    private final Long postId;

    /**
     * 게시판 타입
     */
    private final BoardType boardType;

    /**
     * 게시글 작성자 ID
     */
    private final Long memberId;

    /**
     * 이벤트 발생 시각
     */
    private final LocalDateTime createdAt;

    /**
     * PostCreatedEvent 생성자
     *
     * @param source 이벤트 소스 (일반적으로 이벤트를 발행하는 객체)
     * @param postId 생성된 게시글 ID
     * @param boardType 게시판 타입
     * @param memberId 작성자 ID
     */
    public PostCreatedEvent(Object source, Long postId, BoardType boardType, Long memberId) {
        super(source);
        this.postId = postId;
        this.boardType = boardType;
        this.memberId = memberId;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 간편 생성자 (source를 postId로 사용)
     *
     * @param postId 생성된 게시글 ID
     * @param boardType 게시판 타입
     * @param memberId 작성자 ID
     */
    public PostCreatedEvent(Long postId, BoardType boardType, Long memberId) {
        this(postId, postId, boardType, memberId);
    }

    /**
     * 이벤트 정보를 문자열로 반환
     *
     * @return 이벤트 정보 문자열
     */
    @Override
    public String toString() {
        return String.format("PostCreatedEvent{postId=%d, boardType=%s, memberId=%d, createdAt=%s}",
                postId, boardType, memberId, createdAt);
    }
}