package com.kakaobase.snsapp.domain.posts.controller;

import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.posts.converter.PostConverter;
import com.kakaobase.snsapp.domain.posts.dto.PostRequestDto;
import com.kakaobase.snsapp.domain.posts.dto.PostResponseDto;
import com.kakaobase.snsapp.domain.posts.entity.Post;
import com.kakaobase.snsapp.domain.posts.exception.PostErrorCode;
import com.kakaobase.snsapp.domain.posts.exception.PostException;
import com.kakaobase.snsapp.domain.posts.service.PostLikeService;
import com.kakaobase.snsapp.domain.posts.service.PostService;
import com.kakaobase.snsapp.global.common.response.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 게시글 관련 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@Tag(name = "게시글 API", description = "게시글 CRUD 및 좋아요 기능을 제공하는 API")
public class PostController {

    private final PostService postService;
    private final PostLikeService postLikeService;

    /**
     * 게시글 목록을 조회합니다.
     * 커서 기반 페이지네이션을 적용합니다.
     */
    @GetMapping("/{postType}")
    @Operation(summary = "게시글 목록 조회", description = "게시판 유형별로 게시글 목록을 조회합니다.")
    @PreAuthorize("isAuthenticated() && @accessChecker.hasAccessToBoard(#postType, authentication.principal)")
    public ResponseEntity<PostResponseDto.PostListResponse> getPosts(
            @Parameter(description = "게시판 유형") @PathVariable String postType,
            @Parameter(description = "한 페이지에 표시할 게시글 수") @RequestParam(defaultValue = "12") int limit,
            @Parameter(description = "마지막으로 조회한 게시글 ID") @RequestParam(required = false) Long cursor,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {

        Long memberId = Long.valueOf(userDetails.getId());

        PostResponseDto.PostListResponse response = postService.getPostList(postType, limit, cursor, memberId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{postType}/{postId}")
    @Operation(summary = "게시글 상세 조회", description = "게시글의 상세 정보를 조회합니다.")
    @PreAuthorize("isAuthenticated() && @accessChecker.hasAccessToBoard(#postType, authentication.principal)")
    public ResponseEntity<PostResponseDto.PostDetailResponse> getPostDetail(
            @Parameter(description = "게시판 유형") @PathVariable String postType,
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getId());

        PostResponseDto.PostDetailResponse response = postService.getPostDetail(postId, memberId);

        return ResponseEntity.ok(response);
    }

    /**
     * 새 게시글을 생성합니다.
     */
    @PostMapping("/{postType}")
    @Operation(summary = "게시글 생성", description = "새 게시글을 생성합니다.")
    @PreAuthorize("isAuthenticated() && @accessChecker.hasAccessToBoard(#postType, authentication.principal)")
    public ResponseEntity<PostResponseDto.PostCreateResponse> createPost(
            @Parameter(description = "게시판 유형") @PathVariable String postType,
            @Valid @RequestBody PostRequestDto.PostCreateRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {

        Long memberId = Long.valueOf(userDetails.getId());

        // 게시글 내용 유효성 검증
        if (requestDto.isEmpty()) {
            throw new PostException(PostErrorCode.EMPTY_POST_CONTENT);
        }

        // 유튜브 URL 유효성 검증
        if (!requestDto.isValidYoutubeUrl()) {
            throw new PostException(PostErrorCode.INVALID_YOUTUBE_URL);
        }

        // 게시판 타입 변환
        Post.BoardType boardType = PostConverter.toBoardType(postType);

        // 게시글 생성
        Post createdPost = postService.createPost(boardType, requestDto, memberId);

        // 작성자 정보 조회
        Map<String, String> userInfo = postService.getMemberInfo(memberId);

        String ImageUrl = requestDto.image_url();

        // 응답 생성
        PostResponseDto.PostCreateResponse response = PostConverter.toPostCreateResponse(
                createdPost, userInfo, ImageUrl,false);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 게시글을 삭제합니다.
     */
    @DeleteMapping("/{postType}/{postId}")
    @Operation(summary = "게시글 삭제", description = "게시글을 삭제합니다.")
    @PreAuthorize("@accessChecker.hasAccessToBoard(#postType, authentication.principal) and @accessChecker.isPostOwner(#postId, authentication.principal)")
    public ResponseEntity<PostResponseDto.PostDeleteResponse> deletePost(
            @Parameter(description = "게시판 유형") @PathVariable String postType,
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {

        Long memberId = Long.valueOf(userDetails.getId());

        // 게시글 삭제
        postService.deletePost(postId, memberId);

        // 응답 생성
        PostResponseDto.PostDeleteResponse response = PostConverter.toPostDeleteResponse();

        return ResponseEntity.ok(response);
    }

    /**
     * 게시글에 좋아요를 추가합니다.
     */
    @PostMapping("/{postId}/likes")
    @Operation(summary = "게시글 좋아요 추가", description = "게시글에 좋아요를 추가합니다.")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PostResponseDto.PostLikeResponse> addLike(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {

        Long memberId = Long.valueOf(userDetails.getId());

        // 좋아요 추가
        postLikeService.addLike(postId, memberId);

        // 응답 생성
        PostResponseDto.PostLikeResponse response = PostConverter.toPostLikeResponse(true);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 게시글 좋아요를 취소합니다.
     */
    @DeleteMapping("/{postId}/likes")
    @Operation(summary = "게시글 좋아요 취소", description = "게시글 좋아요를 취소합니다.")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PostResponseDto.PostLikeResponse> removeLike(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {

        Long memberId = Long.valueOf(userDetails.getId());

        // 좋아요 취소
        postLikeService.removeLike(postId, memberId);

        // 응답 생성
        PostResponseDto.PostLikeResponse response = PostConverter.toPostLikeResponse(false);

        return ResponseEntity.ok(response);
    }

    /**
     * YouTube 영상 요약 요청 API
     *
     * <p>사용자가 작성한 게시글에 포함된 YouTube URL의 영상을 요약합니다.
     * AI 서버를 통해 영상의 자막을 분석하여 요약본을 생성하고,
     * 생성된 요약을 게시글에 저장한 후 응답합니다.</p>
     *
     * @param postId 요약을 요청할 게시글의 ID
     * @param userDetails 인증된 사용자 정보
     * @return YouTube 영상 요약 결과
     * @throws PostException 게시글을 찾을 수 없거나, 권한이 없거나, YouTube URL이 없는 경우
     */
    @GetMapping("/{postId}/summary")
    @Operation(summary = "YouTube 영상 요약", description = "게시글의 YouTube 영상을 요약합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "YouTube 영상이 요약되었습니다."),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 요청입니다."),
            @ApiResponse(responseCode = "403", description = "본인의 글만 요약 가능합니다."),
            @ApiResponse(responseCode = "404", description = "해당 게시글을 찾을 수 없습니다."),
            @ApiResponse(responseCode = "500", description = "서버에 문제가 발생하였습니다.")
    })
    @PreAuthorize("isAuthenticated() && @accessChecker.isPostOwner(#postId, authentication.principal)")
    public ResponseEntity<CustomResponse<PostResponseDto.YouTubeSummaryResponse>> summarizeYoutube(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // 사용자 인증 정보에서 memberId 추출
        Long memberId = Long.valueOf(userDetails.getId());

        // 게시글의 YouTube 영상 요약 실행
        PostResponseDto.YouTubeSummaryResponse response = postService.summarizeYoutube(postId, memberId);

        // 성공 응답 반환
        return ResponseEntity.ok(CustomResponse.success("유튜브 요약에 성공하였습니다.", response));
    }
}