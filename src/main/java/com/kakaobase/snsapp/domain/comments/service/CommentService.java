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
import com.kakaobase.snsapp.domain.comments.repository.RecommentRepository;
import com.kakaobase.snsapp.domain.follow.repository.FollowRepository;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import com.kakaobase.snsapp.domain.posts.entity.Post;
import com.kakaobase.snsapp.domain.posts.exception.PostException;
import com.kakaobase.snsapp.domain.posts.repository.PostRepository;
import com.kakaobase.snsapp.domain.posts.service.PostService;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    private static final int DEFAULT_PAGE_SIZE = 12;
    private final CommentLikeRepository commentLikeRepository;
    private final FollowRepository followRepository;
    private final EntityManager em;

    private final BotRecommentService botRecommentService;
    private final PostRepository postRepository;

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

            if(!commentRepository.existsById(request.parent_id())) {
                throw new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "parentId");
            }

            Comment proxyComment = em.getReference(Comment.class, request.parent_id());

            // 대댓글 엔티티 생성 및 저장
            Recomment recomment = commentConverter.toRecommentEntity(proxyComment, proxyMember, request);
            Recomment savedRecomment = recommentRepository.save(recomment);

            //부모 댓글 대댓글 카운트 증가
            commentRepository.incrementRecommentCount(request.parent_id());

            return commentConverter.toCreateRecommentResponse(savedRecomment);
        }

        // 일반 댓글인 경우
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "postId"));
        Comment comment = commentConverter.toCommentEntity(post, proxyMember, request);
        Comment savedComment = commentRepository.save(comment);

        //게시글의 댓글 수 추가
        postRepository.incrementCommentCount(postId);

        log.info("댓글 생성 완료: 댓글 ID={}, 작성자 ID={}, 게시글 ID={}",
                savedComment.getId(), memberId, postId);

        // 게시물 작성자가 소셜봇이면 소셜봇 대댓글 로직 구현하도록
        if (post.getMember().getRole().equals("BOT")) {
            log.info("🤖 [Trigger] 소셜봇 게시글이므로 트리거 실행!");
            botRecommentService.triggerAsync(post, savedComment);
        } else {
            log.info("🙅 [Skip] 게시글 작성자가 소셜봇이 아님 → 트리거 생략");
        }

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
        recommentRepository.softDeleteRecommentsByCommentId(commentId);

        // 댓글 삭제 (Soft Delete)
        comment.softDelete();

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
        Recomment recomment = recommentRepository.findByIdAndDeletedAtIsNull(recommentId)
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


        Member currentUser = em.getReference(Member.class, memberId);
        Set<Long> followingIdSet = followRepository.findFollowingUserIdsByFollowerUser(currentUser);

        // 응답 DTO 생성
        return commentConverter.toRecommentListResponse(
                recomments,
                memberId,
                likedRecommentIdsSet,
                followingIdSet,
                nextCursor
        );
    }


    //특정 유저의 댓글 조회
    @Transactional(readOnly = true)
    public List<CommentResponseDto.CommentInfo> getUserCommentList(int limit, Long cursor, Long memberId) {

        if(!memberRepository.existsById(memberId)) {
            throw new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "userId");
        }

        if (limit < 1) {
            throw new PostException(GeneralErrorCode.INVALID_QUERY_PARAMETER, "limit");
        }

        Pageable pageable = PageRequest.of(0, limit);

        // 댓글 목록 조회
        List<Comment> comments = commentRepository.findByMemberIdWithCursor(memberId, cursor, pageable); // 다음 페이지 확인을 위해 limit + 1개 조회


        // 개별 댓글 정보를 가져와서 CommentInfo 리스트 생성
        List<CommentResponseDto.CommentInfo> commentInfoList = comments.stream()
                .map(comment -> getCommentInfo(memberId, comment.getId()))
                .collect(Collectors.toList());

        // CommentListResponse 생성하여 반환
        return commentInfoList;
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
                .orElseThrow(() -> new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "commentId"));

        // 댓글 좋아요 여부 확인
        boolean isLiked = commentLikeRepository.existsByMemberIdAndCommentId(memberId, commentId);

        // 댓글 작성자 확인 (본인 작성 여부)
        boolean isMine = comment.getMember().getId().equals(memberId);

        Member follower = em.getReference(Member.class, memberId);
        Member following = em.getReference(Member.class, comment.getMember().getId());

        //팔로우 여부 확인
        boolean isFollowing = followRepository.existsByFollowerUserAndFollowingUser(follower, following);

        // CommentInfo 생성
        return commentConverter.toCommentInfo(
                comment,
                isMine,
                isLiked,
                isFollowing
        );
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