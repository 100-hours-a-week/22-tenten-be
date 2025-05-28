package com.kakaobase.snsapp.domain.comments.service;

import com.kakaobase.snsapp.domain.comments.converter.CommentConverter;
import com.kakaobase.snsapp.domain.comments.dto.CommentRequestDto;
import com.kakaobase.snsapp.domain.comments.dto.CommentResponseDto;
import com.kakaobase.snsapp.domain.comments.entity.Comment;
import com.kakaobase.snsapp.domain.comments.entity.Recomment;
import com.kakaobase.snsapp.domain.comments.event.CommentCreatedEvent;
import com.kakaobase.snsapp.domain.comments.exception.CommentErrorCode;
import com.kakaobase.snsapp.domain.comments.exception.CommentException;
import com.kakaobase.snsapp.domain.comments.repository.CommentLikeRepository;
import com.kakaobase.snsapp.domain.comments.repository.CommentRepository;
import com.kakaobase.snsapp.domain.comments.repository.RecommentRepository;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import com.kakaobase.snsapp.domain.posts.entity.Post;
import com.kakaobase.snsapp.domain.posts.service.PostService;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 댓글 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final RecommentRepository recommentRepository;
    private final MemberRepository memberRepository;
    private final CommentConverter commentConverter;
    private final PostService postService;
    private final CommentLikeService commentLikeService;
    private final ApplicationEventPublisher eventPublisher;

    private static final int DEFAULT_PAGE_SIZE = 12;
    private final CommentLikeRepository commentLikeRepository;

    /**
     * 댓글을 생성합니다.
     *
     * @param memberId 회원 ID
     * @param postId 게시글 ID
     * @param request 댓글 생성 요청 DTO
     * @return 생성된 댓글 응답 DTO
     */
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
        Post post = postService.findById(postId);

        // 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "memberId", "회원을 찾을 수 없습니다."));

        // 대댓글인 경우
        if (request.parent_id() != null) {
            Comment parentComment = commentRepository.findByIdAndDeletedAtIsNull(request.parent_id())
                    .orElseThrow(() -> new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "parent_id", "부모 댓글을 찾을 수 없습니다."));

            // 부모 댓글이 같은 게시글에 속하는지 확인
            if (!parentComment.getPost().getId().equals(postId)) {
                throw new CommentException(CommentErrorCode.INVALID_PARENT_COMMENT, "parent_id", "부모 댓글이 다른 게시글에 속합니다.");
            }

            // 대댓글 엔티티 생성 및 저장
            Recomment recomment = commentConverter.toRecommentEntity(parentComment, member, request);
            Recomment savedRecomment = recommentRepository.save(recomment);

            //부모 댓글 대댓글 카운트 증가
            parentComment.increaseRecommentCount();

            log.info("대댓글 생성 완료: 대댓글 ID={}, 작성자 ID={}, 부모 댓글 ID={}",
                    savedRecomment.getId(), memberId, parentComment.getId());

            return commentConverter.toCreateRecommentResponse(savedRecomment);
        }

        // 일반 댓글인 경우
        Comment comment = commentConverter.toCommentEntity(post, member, request);
        Comment savedComment = commentRepository.save(comment);

        //게시글의 댓글 수 추가
        post.increaseCommentCount();

        log.info("댓글 생성 완료: 댓글 ID={}, 작성자 ID={}, 게시글 ID={}",
                savedComment.getId(), memberId, postId);

        // 댓글 생성 이벤트 발행 (대댓글이 아닌 경우에만)
        CommentCreatedEvent event = new CommentCreatedEvent(
                savedComment.getId(),
                postId,
                post.getMemberId(),  // 게시글 작성자 ID
                memberId,  // 댓글 작성자 ID
                savedComment.getContent(),
                savedComment.getCreatedAt()
        );
        eventPublisher.publishEvent(event);

        log.debug("댓글 생성 이벤트 발행: {}", event);

        return commentConverter.toCreateCommentResponse(savedComment);
    }

    /**
     * 댓글을 삭제합니다.
     *
     * @param memberId 현재 로그인한 회원 ID
     * @param commentId 삭제할 댓글 ID
     */
    @Transactional
    public void deleteComment(Long memberId, Long commentId) {
        // 댓글 조회
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "commentId", "삭제할 댓글을 찾을 수 없습니다."));

        // 댓글 작성자 확인
        if (!comment.getMember().getId().equals(memberId)) {
            throw new CommentException(CommentErrorCode.POST_NOT_AUTHORIZED, "commentId", "본인이 작성한 댓글만 삭제할 수 있습니다.");
        }

        // 게시글의 댓글 수 1감소
        Post post = postService.findById(comment.getPost().getId());
        post.decreaseCommentCount();

        // 댓글의 좋아요 삭제
        commentLikeService.deleteAllCommentLikesByCommentId(commentId);

        // 댓글에 달린 모든 대댓글 삭제 (삭제된 것 포함)
        List<Recomment> recomments = recommentRepository.findAllByCommentId(commentId);
        for (Recomment recomment : recomments) {
            commentLikeService.deleteAllRecommentLikesByRecommentId(recomment.getId());
            recommentRepository.delete(recomment);
        }

        // 댓글 삭제 (Soft Delete)
        comment.softDelete();
        commentRepository.save(comment);

        log.info("댓글 삭제 완료: 댓글 ID={}, 삭제자 ID={}", commentId, memberId);
    }

    /**
     * 대댓글을 삭제합니다.
     *
     * @param memberId 현재 로그인한 회원 ID
     * @param recommentId 삭제할 대댓글 ID
     */
    @Transactional
    public void deleteRecomment(Long memberId, Long recommentId) {
        // 대댓글 조회
        Recomment recomment = recommentRepository.findById(recommentId)
                .orElseThrow(() -> new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "recommentId", "해당 대댓글을 찾을 수 없습니다."));

        // 대댓글의 좋아요 삭제
        commentLikeService.deleteAllRecommentLikesByRecommentId(recommentId);

        recomment.onPreRemove();

        // 대댓글 삭제 (Soft Delete)
        recomment.softDelete();

        log.info("대댓글 삭제 완료: 대댓글 ID={}, 삭제자 ID={}", recommentId, memberId);
    }

    /**
     * 게시글의 댓글 목록을 조회합니다.
     *
     * @param memberId 현재 로그인한 회원 ID
     * @param postId 게시글 ID
     * @param pageRequest 페이지 요청 DTO
     * @return 댓글 목록 응답 DTO
     */
    /**
     * 게시글에 달린 댓글 목록을 조회합니다.
     *
     * @param memberId 현재 로그인한 회원 ID
     * @param postId 조회할 게시글 ID
     * @param pageRequest 페이지 요청 정보
     * @return 댓글 목록 응답 DTO
     */
    /**
     * 게시글에 달린 댓글 목록을 조회합니다.
     *
     * @param memberId 현재 로그인한 회원 ID
     * @param postId 조회할 게시글 ID
     * @param pageRequest 페이지 요청 정보
     * @return 댓글 목록 응답 DTO
     */
    public CommentResponseDto.CommentListResponse getCommentsByPostId(Long memberId, Long postId, CommentRequestDto.CommentPageRequest pageRequest) {
        // 게시글 존재 확인
        Post post = postService.findById(postId);

        // 페이지 설정
        int limit = pageRequest.limit() != null ? pageRequest.limit() : DEFAULT_PAGE_SIZE;

        // 댓글 목록 조회
        List<Comment> comments = commentRepository.findByPostIdWithCursor(postId, pageRequest.cursor(), limit + 1); // 다음 페이지 확인을 위해 limit + 1개 조회

        if (comments.isEmpty()) {
            return new CommentResponseDto.CommentListResponse(
                    Collections.emptyList(),
                    false,
                    null
            );
        }

        // 다음 페이지 존재 여부 확인
        boolean hasNext = comments.size() > limit;

        // 실제 반환할 댓글 목록 (limit개로 제한)
        List<Comment> pageComments = hasNext ? comments.subList(0, limit) : comments;

        // 다음 커서 설정
        Long nextCursor = hasNext ? pageComments.get(pageComments.size() - 1).getId() : null;

        // 개별 댓글 정보를 가져와서 CommentInfo 리스트 생성
        List<CommentResponseDto.CommentInfo> commentInfoList = pageComments.stream()
                .map(comment -> getCommentInfo(memberId, comment.getId()))
                .collect(Collectors.toList());

        // CommentListResponse 생성하여 반환
        return new CommentResponseDto.CommentListResponse(
                commentInfoList,
                hasNext,
                nextCursor
        );
    }


    /**
     * 댓글의 대댓글 목록을 조회합니다.
     *
     * @param memberId 현재 로그인한 회원 ID
     * @param commentId 댓글 ID
     * @param pageRequest 페이지 요청 DTO
     * @return 대댓글 목록 응답 DTO
     */
    public CommentResponseDto.RecommentListResponse getRecommentsByCommentId(Long memberId, Long commentId, CommentRequestDto.RecommentPageRequest pageRequest) {
        // 댓글 존재 확인
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "commentId", "해당 댓글을 찾을 수 없습니다."));

        // 페이지 설정
        int limit = pageRequest.limit() != null ? pageRequest.limit() : DEFAULT_PAGE_SIZE;

        // 대댓글 목록 조회
        List<Recomment> recomments = recommentRepository.findByRecommentIdWithCursor(commentId, pageRequest.cursor(), limit);

        if (recomments.isEmpty()) {
            return new CommentResponseDto.RecommentListResponse(
                    Collections.emptyList(),
                    false,
                    null
            );
        }

        // 다음 페이지 존재 여부 확인
        boolean hasNext = recomments.size() >= limit;
        Long nextCursor = hasNext ? recomments.get(recomments.size() - 1).getId() : null;

        // 대댓글 ID 추출
        List<Long> recommentIds = recomments.stream()
                .map(Recomment::getId)
                .collect(Collectors.toList());

        // 대댓글 좋아요 정보 조회
        List<Long> likedRecommentIds = recommentRepository.findLikedRecommentIds(recommentIds, memberId);
        Set<Long> likedRecommentIdsSet = new HashSet<>(likedRecommentIds);

        // 응답 DTO 생성
        return commentConverter.toRecommentListResponse(
                recomments,
                memberId,
                likedRecommentIdsSet,
                nextCursor
        );
    }

    public CommentResponseDto.CommentDetailResponse getCommentDetail(Long memberId, Long commentId) {
        CommentResponseDto.CommentInfo commentInfo = getCommentInfo(memberId, commentId);
        return new CommentResponseDto.CommentDetailResponse(commentInfo);
    }


    /**
     * 댓글 정보를 반환
     *
     * @param memberId 현재 로그인한 회원 ID
     * @param commentId 조회할 댓글 ID
     * @return 댓글 상세 응답 DTO
     */
    public CommentResponseDto.CommentInfo getCommentInfo(Long memberId, Long commentId) {
        // 댓글 조회
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "commentId", "댓글을 찾을 수 없습니다."));

        // 댓글 좋아요 여부 확인
        boolean isLiked = commentLikeRepository.existsByMemberIdAndCommentId(memberId, commentId);

        // 댓글 작성자 확인 (본인 작성 여부)
        boolean isMine = comment.getMember().getId().equals(memberId);

        // CommentInfo 생성
        CommentResponseDto.CommentInfo commentInfo = commentConverter.toCommentInfo(
                comment,
                isMine,
                isLiked
        );

        return commentInfo;
    }

    /**
     * 댓글 ID로 댓글을 조회합니다.
     *
     * @param commentId 댓글 ID
     * @return 댓글 엔티티
     */
    public Comment findById(Long commentId) {
        return commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "commentId", "댓글을 찾을 수 없습니다."));
    }

    /**
     * 대댓글 ID로 대댓글을 조회합니다.
     *
     * @param recommentId 대댓글 ID
     * @return 대댓글 엔티티
     */
    public Recomment findRecommentById(Long recommentId) {
        return recommentRepository.findByIdAndDeletedAtIsNull(recommentId)
                .orElseThrow(() -> new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "recommentId", "대댓글을 찾을 수 없습니다."));
    }
}