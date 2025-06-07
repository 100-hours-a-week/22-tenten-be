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
import com.kakaobase.snsapp.domain.members.converter.MemberConverter;
import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.posts.exception.PostException;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 댓글 및 대댓글 좋아요 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentLikeService {

    private final CommentRepository commentRepository;
    private final RecommentRepository recommentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final RecommentLikeRepository recommentLikeRepository;
    private final MemberConverter memberConverter;
    private final CommentConverter commentConverter;

    /**
     * 댓글에 좋아요를 추가합니다.
     *
     * @param commentId 댓글 ID
     * @param memberId 회원 ID
     * @throws CommentException 댓글이 없거나 이미 좋아요한 경우
     */
    @Transactional
    public void addCommentLike(Long memberId, Long commentId) {
        // 댓글 존재 여부 확인
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "commentId"));

        // 이미 좋아요한 경우 확인
        if (commentLikeRepository.existsByMemberIdAndCommentId(memberId, commentId)) {
            throw new CommentException(CommentErrorCode.ALREADY_LIKED);
        }

        // 좋아요 엔티티 생성 및 저장
        CommentLike commentLike = commentConverter.toCommentLikeEntity(memberId, commentId);
        commentLikeRepository.save(commentLike);

        // 댓글 좋아요 수 증가
        comment.increaseLikeCount();
        commentRepository.save(comment);

        log.info("댓글 좋아요 추가 완료: 댓글 ID={}, 회원 ID={}", commentId, memberId);
    }

    /**
     * 댓글 좋아요를 취소합니다.
     *
     * @param commentId 댓글 ID
     * @param memberId 회원 ID
     * @return 좋아요 응답 DTO
     * @throws CommentException 댓글이 없거나 좋아요하지 않은 경우
     */
    @Transactional
    public void removeCommentLike(Long memberId, Long commentId) {
        // 댓글 존재 여부 확인
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "commentId"));

        // 좋아요 존재 여부 확인
        CommentLike commentLike = commentLikeRepository.findByMemberIdAndCommentId(memberId, commentId)
                .orElseThrow(() -> new CommentException(CommentErrorCode.ALREADY_UNLIKED));

        // 좋아요 삭제
        commentLikeRepository.delete(commentLike);

        // 댓글 좋아요 수 감소
        comment.decreaseLikeCount();
        commentRepository.save(comment);

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
        Recomment recomment = recommentRepository.findByIdAndDeletedAtIsNull(recommentId)
                .orElseThrow(() -> new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "recommentId"));

        // 이미 좋아요한 경우 확인
        if (recommentLikeRepository.existsByMemberIdAndRecommentId(memberId, recommentId)) {
            throw new CommentException(CommentErrorCode.ALREADY_LIKED);
        }

        // 좋아요 엔티티 생성 및 저장
        RecommentLike recommentLike = commentConverter.toRecommentLikeEntity(memberId, recommentId);
        recommentLikeRepository.save(recommentLike);

        // 대댓글 좋아요 수 증가
        recomment.increaseLikeCount();
        recommentRepository.save(recomment);

        log.info("대댓글 좋아요 추가 완료: 대댓글 ID={}, 회원 ID={}", recommentId, memberId);
    }

    /**
     * 대댓글 좋아요를 취소합니다.
     *
     * @param recommentId 대댓글 ID
     * @param memberId 회원 ID
     * @throws CommentException 대댓글이 없거나 좋아요하지 않은 경우
     */
    @Transactional
    public void removeRecommentLike(Long memberId, Long recommentId) {
        // 대댓글 존재 여부 확인
        Recomment recomment = recommentRepository.findByIdAndDeletedAtIsNull(recommentId)
                .orElseThrow(() -> new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "recommentId"));

        // 좋아요 존재 여부 확인
        RecommentLike recommentLike = recommentLikeRepository.findByMemberIdAndRecommentId(memberId, recommentId)
                .orElseThrow(() -> new CommentException(CommentErrorCode.ALREADY_UNLIKED));

        // 좋아요 삭제
        recommentLikeRepository.delete(recommentLike);

        // 대댓글 좋아요 수 감소
        recomment.decreaseLikeCount();
        recommentRepository.save(recomment);

        log.info("대댓글 좋아요 취소 완료: 대댓글 ID={}, 회원 ID={}", recommentId, memberId);

    }


    /**
     * 회원이 특정 댓글 목록 중 좋아요한 댓글 ID 목록을 조회합니다.
     *
     * @param memberId 회원 ID
     * @param commentIds 댓글 ID 목록
     * @return 좋아요한 댓글 ID 목록
     */
    public List<Long> getLikedCommentIdsByMember(Long memberId, List<Long> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) {
            return Collections.emptyList();
        }

        return commentLikeRepository.findCommentIdsByMemberIdAndCommentIdIn(memberId, commentIds);
    }

    /**
     * 회원이 특정 대댓글 목록 중 좋아요한 대댓글 ID 목록을 조회합니다.
     *
     * @param memberId 회원 ID
     * @param recommentIds 대댓글 ID 목록
     * @return 좋아요한 대댓글 ID 목록
     */
    public List<Long> getLikedRecommentIdsByMember(Long memberId, List<Long> recommentIds) {
        if (recommentIds == null || recommentIds.isEmpty()) {
            return Collections.emptyList();
        }

        return recommentLikeRepository.findRecommentIdsByMemberIdAndRecommentIdIn(memberId, recommentIds);
    }

    /**
     * 댓글 삭제 시 연관된 좋아요를 일괄 삭제합니다.
     *
     * @param commentId 댓글 ID
     */
    @Transactional
    public void deleteAllCommentLikesByCommentId(Long commentId) {
        int deletedCount = commentLikeRepository.deleteByCommentId(commentId);
        log.info("댓글 관련 좋아요 일괄 삭제 완료: 댓글 ID={}, 삭제된 좋아요 수={}", commentId, deletedCount);
    }

    /**
     * 대댓글 삭제 시 연관된 좋아요를 일괄 삭제합니다.
     *
     * @param recommentId 대댓글 ID
     */
    @Transactional
    public void deleteAllRecommentLikesByRecommentId(Long recommentId) {
        int deletedCount = recommentLikeRepository.deleteByRecommentId(recommentId);
        log.info("대댓글 관련 좋아요 일괄 삭제 완료: 대댓글 ID={}, 삭제된 좋아요 수={}", recommentId, deletedCount);
    }

    /**
     * 댓글이 삭제될 때 해당 댓글에 달린 모든 대댓글의 좋아요를 일괄 삭제합니다.
     *
     * @param commentId 댓글 ID
     */
    @Transactional
    public void deleteAllRecommentLikesByParentCommentId(Long commentId) {
        // 대댓글 ID 목록 조회
        List<Long> recommentIds = recommentRepository.findByCommentId(commentId)
                .stream()
                .map(Recomment::getId)
                .toList();

        if (!recommentIds.isEmpty()) {
            int deletedCount = recommentLikeRepository.deleteByRecommentIdIn(recommentIds);
            log.info("댓글 관련 대댓글 좋아요 일괄 삭제 완료: 댓글 ID={}, 삭제된 좋아요 수={}", commentId, deletedCount);
        }
    }


    @Transactional(readOnly = true)
    public List<MemberResponseDto.UserInfo> getCommentLikedMembers(Long commentId, int limit, Long cursor) {
        if(!commentRepository.existsById(commentId)){
            throw new PostException(GeneralErrorCode.RESOURCE_NOT_FOUND);
        }

        List<Member> members = commentLikeRepository.findMembersByCommentIdWithCursor(commentId, cursor, limit);

        List<MemberResponseDto.UserInfo> result = memberConverter.convertToUserInfoList(members);

        return result;
    }

    @Transactional(readOnly = true)
    public List<MemberResponseDto.UserInfo> getRecommentLikedMembers(Long recommentId, int limit, Long cursor) {
        if(!recommentRepository.existsById(recommentId)){
            throw new PostException(GeneralErrorCode.RESOURCE_NOT_FOUND);
        }

        List<Member> members = recommentLikeRepository.findMembersByRecommentIdWithCursor(recommentId, cursor, limit);

        List<MemberResponseDto.UserInfo> result = memberConverter.convertToUserInfoList(members);

        return result;
    }
}