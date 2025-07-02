package com.kakaobase.snsapp.domain.comments.service;

import com.kakaobase.snsapp.domain.comments.converter.CommentConverter;
import com.kakaobase.snsapp.domain.comments.entity.Comment;
import com.kakaobase.snsapp.domain.comments.entity.CommentLike;
import com.kakaobase.snsapp.domain.comments.entity.Recomment;
import com.kakaobase.snsapp.domain.comments.entity.RecommentLike;
import com.kakaobase.snsapp.domain.comments.exception.CommentErrorCode;
import com.kakaobase.snsapp.domain.comments.exception.CommentException;
import com.kakaobase.snsapp.domain.comments.repository.CommentLikeRepository;
import com.kakaobase.snsapp.domain.comments.repository.CommentRepository;
import com.kakaobase.snsapp.domain.comments.repository.RecommentLikeRepository;
import com.kakaobase.snsapp.domain.comments.repository.RecommentRepository;
import com.kakaobase.snsapp.domain.comments.service.cache.CommentCacheService;
import com.kakaobase.snsapp.domain.members.converter.MemberConverter;
import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.notification.service.NotificationService;
import com.kakaobase.snsapp.domain.posts.exception.PostException;
import com.kakaobase.snsapp.global.common.redis.error.CacheException;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 댓글 및 대댓글 좋아요 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentLikeService {

    private final CommentRepository commentRepository;
    private final RecommentRepository recommentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final RecommentLikeRepository recommentLikeRepository;
    private final CommentConverter commentConverter;
    private final CommentCacheService commentCacheService;
    private final NotificationService notifService;
    private final EntityManager em;
    private final MemberConverter memberConverter;

    /**
     * 댓글에 좋아요를 추가합니다.
     */
    @Transactional
    public void addCommentLike(Long memberId, Long commentId) {
        // 댓글 존재 여부 확인
        if(!commentRepository.existsById(commentId)) {
            throw new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "commentId");
        }

        // 이미 좋아요한 경우 확인
        if (commentLikeRepository.existsByMemberIdAndCommentId(memberId, commentId)) {
            throw new CommentException(CommentErrorCode.ALREADY_LIKED);
        }

        // 좋아요 엔티티 생성 및 저장
        Member proxyMember = em.getReference(Member.class, memberId);
        Comment proxyComment = em.getReference(Comment.class, commentId);

        CommentLike commentLike = new CommentLike(proxyMember, proxyComment);
        commentLikeRepository.save(commentLike);

        // 댓글 좋아요 수 증가
        try{
            commentCacheService.incrementLikeCount(commentId);
        } catch (CacheException e){
            log.error(e.getMessage());
            Comment comment = em.find(Comment.class, commentId);
            comment.increaseLikeCount();
        }

        log.info("댓글 좋아요 추가 완료: 댓글 ID={}, 회원 ID={}", commentId, memberId);

        if(!proxyComment.getMember().getId().equals(memberId)) {
            MemberResponseDto.UserInfo userInfo = memberConverter.toUserInfo(proxyMember);
            notifService.sendPostLikeCreatedNotification(
                    proxyComment.getMember().getId(),
                    commentId,
                    null,
                    userInfo,
                    proxyComment.getPost().getId());
        }
    }

    /**
     * 댓글 좋아요를 취소합니다.
     */
    @Transactional
    public void removeCommentLike(Long memberId, Long commentId) {
        // 댓글 존재 여부 확인
        if(!commentRepository.existsById(commentId)) {
            throw new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "commentId");
        }

        // 좋아요 존재 여부 확인
        CommentLike commentLike = commentLikeRepository.findByMemberIdAndCommentId(memberId, commentId)
                .orElseThrow(() -> new CommentException(CommentErrorCode.ALREADY_UNLIKED));

        // 좋아요 삭제
        commentLikeRepository.delete(commentLike);

        // 댓글 좋아요 수 감소
        try{
            commentCacheService.decrementLikeCount(commentId);
        } catch (CacheException e){
            log.error(e.getMessage());
            Comment comment = em.find(Comment.class, commentId);
            comment.decreaseLikeCount();
        }

        log.info("댓글 좋아요 취소 완료: 댓글 ID={}, 회원 ID={}", commentId, memberId);
    }

    /**
     * 대댓글에 좋아요를 추가합니다.
     *
     * @param recommentId 대댓글 ID
     * @param memberId 회원 ID
     * @throws CommentException 대댓글이 없거나 이미 좋아요한 경우
     */
    @Transactional
    public void addRecommentLike(Long memberId, Long recommentId) {
        // 대댓글 존재 여부 확인
        Recomment recomment = recommentRepository.findById(recommentId)
                .orElseThrow(() -> new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "recommentId"));

        // 이미 좋아요한 경우 확인
        if (recommentLikeRepository.existsByMemberIdAndRecommentId(memberId, recommentId)) {
            throw new CommentException(CommentErrorCode.ALREADY_LIKED);
        }


        Member proxyMember = em.getReference(Member.class, memberId);
        Recomment proxyRecomment = em.getReference(Recomment.class, recommentId);

        // 좋아요 엔티티 생성 및 저장
        RecommentLike recommentLike = commentConverter.toRecommentLikeEntity(memberId, recommentId);
        recommentLikeRepository.save(recommentLike);

        // 대댓글 좋아요 수 증가
        recomment.increaseLikeCount();

        log.info("대댓글 좋아요 추가 완료: 대댓글 ID={}, 회원 ID={}", recommentId, memberId);

        if(!proxyRecomment.getMember().getId().equals(memberId)){
            MemberResponseDto.UserInfo userInfo = memberConverter.toUserInfo(proxyMember);
            notifService.sendRecommentLikeCreatedNotification(
                    proxyRecomment.getMember().getId(),
                    recommentId,
                    null,
                    userInfo,
                    proxyRecomment.getComment().getPost().getId());
        }
    }

    /**
     * 대댓글 좋아요를 취소합니다.
     *
     */
    @Transactional
    public void removeRecommentLike(Long memberId, Long recommentId) {
        // 대댓글 존재 여부 확인
        Recomment recomment = recommentRepository.findById(recommentId)
                .orElseThrow(() -> new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "recommentId"));

        // 좋아요 존재 여부 확인
        RecommentLike recommentLike = recommentLikeRepository.findByMemberIdAndRecommentId(memberId, recommentId)
                .orElseThrow(() -> new CommentException(CommentErrorCode.ALREADY_UNLIKED));

        // 좋아요 삭제
        recommentLikeRepository.delete(recommentLike);

        // 대댓글 좋아요 수 감소
        recomment.decreaseLikeCount();

        log.info("대댓글 좋아요 취소 완료: 대댓글 ID={}, 회원 ID={}", recommentId, memberId);
    }

    /**
     * 특정 댓글에 좋아요를 누른 회원 정보 조회
     */
    @Transactional(readOnly = true)
    public List<MemberResponseDto.UserInfo> getCommentLikedMembers(Long commentId, int limit, Long cursor) {
        if(!commentRepository.existsById(commentId)){
            throw new PostException(GeneralErrorCode.RESOURCE_NOT_FOUND);
        }

        return commentLikeRepository.findMembersByCommentIdWithCursor(commentId, cursor, limit);
    }

    /**
     * 특정 대댓글에 좋아요를 누른 회원 정보 조회
     */
    @Transactional(readOnly = true)
    public List<MemberResponseDto.UserInfo> getRecommentLikedMembers(Long recommentId, int limit, Long cursor) {
        if(!recommentRepository.existsById(recommentId)){
            throw new PostException(GeneralErrorCode.RESOURCE_NOT_FOUND);
        }

        return recommentLikeRepository.findMembersByRecommentIdWithCursor(recommentId, cursor, limit);
    }
}