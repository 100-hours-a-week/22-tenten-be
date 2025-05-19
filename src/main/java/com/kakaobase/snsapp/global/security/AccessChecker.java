package com.kakaobase.snsapp.global.security;

import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.comments.repository.CommentRepository;
import com.kakaobase.snsapp.domain.comments.repository.RecommentRepository;
import com.kakaobase.snsapp.domain.posts.converter.PostConverter;
import com.kakaobase.snsapp.domain.posts.entity.Post;
import com.kakaobase.snsapp.domain.posts.exception.PostException;
import com.kakaobase.snsapp.domain.posts.repository.PostRepository;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import com.kakaobase.snsapp.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 게시판 및 게시글 접근 권한을 검증하는 클래스
 * Spring Security의 @PreAuthorize 어노테이션과 함께 사용됩니다.
 */
@Slf4j
@Component("accessChecker")
@RequiredArgsConstructor
public class AccessChecker {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final RecommentRepository recommentRepository;

    /**
     * 사용자가 특정 게시판에 접근할 권한이 있는지 검증합니다.
     *
     * @param postType 게시판 타입 (예: all, pangyo_1, jeju_2)
     * @param userDetails 인증된 사용자 정보
     * @return 접근 가능하면 true, 아니면 false
     */
    public boolean hasAccessToBoard(String postType, CustomUserDetails userDetails) {
        // 인증되지 않은 사용자는 'all' 게시판만 접근 가능
        if (userDetails == null) {
            return "all".equalsIgnoreCase(postType);
        }

        // 관리자, 봇 권한이 있는 경우 모든 게시판 접근 가능
        if (isAdminOrBot(userDetails)) {
            return true;
        }

        // 'all' 게시판은 모든 인증된 사용자 접근 가능
        if ("all".equalsIgnoreCase(postType)) {
            return true;
        }

        // 사용자의 기수(className) 확인
        String className = userDetails.getClassName();

        if (!StringUtils.hasText(className)) {
            Long memberId = Long.valueOf(userDetails.getId());
            log.warn("사용자 ID {}의 기수 정보가 없습니다.", memberId);
            return false;
        }

        // postType과 사용자의 기수 일치 여부 확인
        String normalizedPostType = postType.toUpperCase();
        boolean hasAccess = className.equals(normalizedPostType);

        if (!hasAccess) {
            log.debug("사용자 ID {}(기수: {})의 게시판 접근 거부: {}",
                    Long.valueOf(userDetails.getId()), className, postType);
            throw new CustomException(GeneralErrorCode.FORBIDDEN);
        }

        return hasAccess;
    }

    /**
     * 사용자가 게시글의 소유자인지 검증합니다.
     *
     * @param postId 게시글 ID
     * @param userDetails 인증된 사용자 정보
     * @return 소유자이면 true, 아니면 false
     */
    public boolean isPostOwner(Long postId, CustomUserDetails userDetails) {
        if (userDetails == null) {
            return false;
        }

        // 관리자, 봇 권한이 있는 경우 소유자로 취급
        if (isAdminOrBot(userDetails)) {
            return true;
        }

        // 사용자 ID 확인
        Long memberId = Long.valueOf(userDetails.getId());;
        if (memberId == null) {
            return false;
        }

        // 게시글 조회
        return postRepository.findByIdAndMemberId(postId, memberId).isPresent();
    }

    /**
     * 사용자가 댓글의 소유자인지 검증합니다.
     *
     * @param commentId 댓글 ID
     * @param userDetails 인증된 사용자 정보
     * @return 소유자이면 true, 아니면 false
     */
    public boolean isCommentOwner(Long commentId, CustomUserDetails userDetails) {
        if (userDetails == null) {
            return false;
        }

        // 관리자, 봇 권한이 있는 경우 소유자로 취급
        if (isAdminOrBot(userDetails)) {
            return true;
        }

        // 사용자 ID 확인
        Long memberId = Long.valueOf(userDetails.getId());;
        if (memberId == null) {
            return false;
        }

        // 댓글 조회
        return commentRepository.findByIdAndMemberId(commentId, memberId).isPresent();
    }

    /**
     * 사용자가 게시글의 댓글을 작성할 권한이 있는지 검증합니다.
     *
     * @param postId 게시글 ID
     * @param userDetails 인증된 사용자 정보
     * @return 권한이 있으면 true, 아니면 false
     */
    public boolean canCommentOnPost(Long postId, CustomUserDetails userDetails) {
        // 로그인한 사용자만 댓글 작성 가능
        if (userDetails == null) {
            return false;
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(GeneralErrorCode.RESOURCE_NOT_FOUND, "postId"));

        // 게시글이 속한 게시판에 접근 권한이 있는지 확인
        return hasAccessToBoard(post.getBoardType().name(), userDetails);
    }

    /**
     * 대댓글 소유자인지 확인
     *
     * @param recommentId 대댓글 ID
     * @param userDetails 인증된 사용자 정보
     * @return 대댓글 소유자 여부
     */
    public boolean isRecommentOwner(Long recommentId, CustomUserDetails userDetails) {
        if (userDetails == null) {
            return false;
        }

        // 관리자, 봇 권한이 있는 경우 소유자로 취급
        if (isAdminOrBot(userDetails)) {
            return true;
        }

        Long memberId = Long.valueOf(userDetails.getId());;
        if (memberId == null) {
            return false;
        }

        // RecommentRepository에서 소유자 확인 쿼리 사용
        return recommentRepository.findByIdAndMemberId(recommentId, memberId).isPresent();
    }

    /**
     * 사용자가 관리자 또는 봇 권한을 가지고 있는지 확인
     *
     * @param userDetails 사용자 정보
     * @return 관리자 또는 봇 권한이 있으면 true
     */
    private boolean isAdminOrBot(CustomUserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority ->
                        authority.equals("ROLE_ADMIN") ||
                                authority.equals("ROLE_BACKEND_BOT") ||
                                authority.equals("ROLE_FRONTEND_BOT")
                );
    }

    /**
     * 문자열 형태의 postType을 BoardType enum으로 변환합니다.
     *
     * @param postType 게시판 타입 문자열
     * @return BoardType enum 값
     * @throws PostException 유효하지 않은 postType인 경우
     */
    public Post.BoardType convertToBoardType(String postType) {
        try {
            return PostConverter.toBoardType(postType);
        } catch (IllegalArgumentException e) {
            throw new PostException(GeneralErrorCode.RESOURCE_NOT_FOUND, "postType");
        }
    }
}