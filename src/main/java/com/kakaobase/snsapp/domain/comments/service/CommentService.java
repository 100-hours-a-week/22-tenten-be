package com.kakaobase.snsapp.domain.comments.service;

import com.kakaobase.snsapp.domain.comments.converter.CommentConverter;
import com.kakaobase.snsapp.domain.comments.dto.CommentRequestDto;
import com.kakaobase.snsapp.domain.comments.dto.CommentResponseDto;
import com.kakaobase.snsapp.domain.comments.entity.Comment;
import com.kakaobase.snsapp.domain.comments.entity.Recomment;
import com.kakaobase.snsapp.domain.comments.exception.CommentErrorCode;
import com.kakaobase.snsapp.domain.comments.exception.CommentException;
import com.kakaobase.snsapp.domain.comments.repository.CommentLikeRepository;
import com.kakaobase.snsapp.domain.comments.repository.CommentRepository;
import com.kakaobase.snsapp.domain.comments.repository.RecommentLikeRepository;
import com.kakaobase.snsapp.domain.comments.repository.RecommentRepository;
import com.kakaobase.snsapp.domain.comments.service.async.CommentAsyncService;
import com.kakaobase.snsapp.domain.comments.service.cache.CommentCacheService;
import com.kakaobase.snsapp.domain.members.converter.MemberConverter;
import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import com.kakaobase.snsapp.domain.notification.service.NotificationService;
import com.kakaobase.snsapp.domain.posts.entity.Post;
import com.kakaobase.snsapp.domain.posts.exception.PostException;
import com.kakaobase.snsapp.domain.posts.repository.PostRepository;
import com.kakaobase.snsapp.domain.posts.service.cache.PostCacheService;
import com.kakaobase.snsapp.global.common.redis.error.CacheException;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 댓글 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final RecommentRepository recommentRepository;
    private final MemberRepository memberRepository;
    private final CommentConverter commentConverter;
    private final PostCacheService postCacheService;

    private final CommentLikeRepository commentLikeRepository;
    private final EntityManager em;

    private final PostRepository postRepository;
    private final CommentCacheService commentCacheService;
    private final RecommentLikeRepository recommentLikeRepository;
    private final CommentAsyncService commentAsyncService;
    private final NotificationService notifService;
    private final MemberConverter memberConverter;

    /**
     * 댓글을 생성합니다.
     *
     * @param memberId 회원 ID
     * @param postId 게시글 ID
     * @param request 댓글 생성 요청 DTO
     * @return 생성된 댓글 응답 DTO
     */
    @Transactional
    public CommentResponseDto.CreateCommentResponse createComment(Long memberId, Long postId, CommentRequestDto.CreateCommentRequest request) {
        // 게시글 존재 확인
        if(!postRepository.existsById(postId)) {
            throw new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "postId");
        }

        Member proxyMember = em.getReference(Member.class, memberId);

        // 대댓글인 경우
        if (request.parent_id() != null) {

            // 1) 존재 여부 확인
            if (!commentRepository.existsById(request.parent_id())) {
                throw new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "parentId");
            }
            // 2) 프록시 로드
            Comment proxyComment = em.getReference(Comment.class, request.parent_id());


            // 3) INSERT 수행
            Recomment recomment = commentConverter.toRecommentEntity(proxyComment, proxyMember, request);
            Recomment savedRecomment = recommentRepository.save(recomment);

            // 4) 예외 상황에서만 부모 댓글 카운트 직접 증가 (DML)
            try {
                commentCacheService.incrementCommentCount(request.parent_id());
            } catch (CacheException e) {
                proxyComment.increaseRecommentCount();
            }
            // 5) 알림
            if (!proxyComment.getMember().getId().equals(memberId)) {
                MemberResponseDto.UserInfo userInfo = memberConverter.toUserInfo(proxyMember);
                notifService.sendRecommentCreatedNotification(
                        proxyComment.getMember().getId(),
                        savedRecomment.getId(),
                        request.content(),
                        userInfo,
                        proxyComment.getPost().getId()
                );
            }

            // 6) BOT 트리거
            Post post = proxyComment.getPost();
            if (post.getMember().getRole().equals(Member.Role.BOT)) {
                log.info("🤖 [Trigger] BOT 작성 게시글에 유저 대댓글 → BOT 후속 대댓글");
                commentAsyncService.triggerAsync(post, proxyComment);
            }

            // 6) 응답 DTO 반환 (대댓글)
            return commentConverter.toCreateRecommentResponse(savedRecomment);
        }

        // 일반 댓글인 경우
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "postId"));
        Comment comment = commentConverter.toCommentEntity(post, proxyMember, request);
        Comment savedComment = commentRepository.save(comment);

        //캐시에 게시글의 댓글 수 추가
        try{
            postCacheService.incrementCommentCount(post.getId());
        }
        catch (CacheException e){
            log.error(e.getMessage());
            post.increaseCommentCount();
        }


        log.info("댓글 생성 완료: 댓글 ID={}, 작성자 ID={}, 게시글 ID={}",
                savedComment.getId(), memberId, postId);

        // 게시물 작성자가 소셜봇이면 소셜봇 대댓글 로직 구현하도록
        if (post.getMember().getRole().equals("BOT")) {
            log.info("🤖 [Trigger] 소셜봇 게시글이므로 BOT 대댓글 생성");
            commentAsyncService.triggerAsync(post, savedComment);
        } else {
            log.info("🙅 [Skip] 게시글 작성자가 소셜봇이 아님 → BOT 대댓글 미실행");
        }

        //알림 전송
        if(!memberId.equals(post.getMember().getId())) {
            var userInfo = memberConverter.toUserInfo(proxyMember);
            notifService.sendCommentCreatedNotification(
                    post.getMember().getId(),
                    savedComment.getId(),
                    request.content(),
                    userInfo,
                    post.getId());
        }

        return commentConverter.toCreateCommentResponse(savedComment);
    }

    /**
     * 댓글을 삭제합니다.
     */
    @Transactional
    public void deleteComment(Long commentId) {
        // 댓글 조회
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "commentId", "삭제할 댓글을 찾을 수 없습니다."));

        recommentLikeRepository.deleteByCommentId(commentId);
        recommentRepository.deleteByCommentId(commentId);
        commentLikeRepository.deleteByCommentId(commentId);

        // 게시글의 댓글 수 1감소
        try{
            postCacheService.decrementCommentCount(comment.getPost().getId());
        } catch (CacheException e){
            log.error(e.getMessage());
            comment.getPost().decreaseCommentCount();
        }


        // 댓글 삭제 (Soft Delete)
        commentRepository.delete(comment);
    }

    /**
     * 대댓글을 삭제합니다.
     */
    @Transactional
    public void deleteRecomment(Long memberId, Long recommentId) {
        // 대댓글 조회
        Recomment recomment = recommentRepository.findById(recommentId)
                .orElseThrow(() -> new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "recommentId", "해당 대댓글을 찾을 수 없습니다."));

        // 대댓글의 좋아요 삭제
        recommentLikeRepository.deleteByRecommentId(recommentId);

        try{
            commentCacheService.decrementCommentCount(recomment.getComment().getId());
        } catch (CacheException e){
            log.error(e.getMessage());
            recomment.getComment().decreaseRecommentCount();
        }

        // 대댓글 삭제 (Soft Delete)
        recommentRepository.delete(recomment);

        log.info("대댓글 삭제 완료: 대댓글 ID={}, 삭제자 ID={}", recommentId, memberId);
    }

    /**
     * 특정 댓글 상세 조회
     */
    @Transactional(readOnly = true)
    public CommentResponseDto.CommentInfo getCommentInfo(Long memberId, Long commentId) {
        return commentRepository.findCommentInfoById(commentId, memberId)
                .orElseThrow(() -> new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "commentId"));
    }

    /**
     * 게시글의 댓글 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<CommentResponseDto.CommentInfo> getCommentsByPostId(Long memberId, Long postId, Integer limit, Long cursor) {
        // 1. 유효성 검증
        if (limit < 1) {
            throw new CommentException(GeneralErrorCode.INVALID_QUERY_PARAMETER, "limit", "limit는 1 이상이어야 합니다.");
        }

        // 2. 게시글 조회
        List<CommentResponseDto.CommentInfo> commentInfos = commentRepository.findCommentInfoListWithCursor(postId, cursor, limit, memberId);

        // 3. PostListItem으로 변환
        return commentConverter.updateWithCachedStats(commentInfos);
    }

    /**
     * 댓글의 대댓글 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<CommentResponseDto.RecommentInfo> getRecommentInfoList(Long memberId, Long commentId, Integer limit, Long cursor) {
        if(limit < 1) {
            throw new CommentException(GeneralErrorCode.INVALID_QUERY_PARAMETER, "limit", "limit는 1 이상이어야 합니다.");
        }

        // 대댓글 목록 조회
        return recommentRepository.findRecommentInfoListWithCursor(commentId, cursor, limit, memberId);
    }

    /**
     * 특정 유저의 댓글 조회
     */
    @Transactional(readOnly = true)
    public List<CommentResponseDto.CommentInfo> getUserCommentList(int limit, Long cursor, Long memberId, Long currentMemberId) {

        if (limit < 1) {
            throw new PostException(GeneralErrorCode.INVALID_QUERY_PARAMETER, "limit");
        }

        if(!memberRepository.existsById(memberId)) {
            throw new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "userId");
        }

        List<CommentResponseDto.CommentInfo> commentInfos = commentRepository.findCommentInfoByMemberWithCursor(memberId, cursor, limit, currentMemberId);

        return commentConverter.updateWithCachedStats(commentInfos);
    }
}