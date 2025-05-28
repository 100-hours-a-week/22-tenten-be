package com.kakaobase.snsapp.domain.comments.controller;

import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.comments.dto.CommentRequestDto;
import com.kakaobase.snsapp.domain.comments.dto.CommentResponseDto;
import com.kakaobase.snsapp.domain.comments.service.CommentService;
import com.kakaobase.snsapp.domain.comments.service.CommentLikeService;
import com.kakaobase.snsapp.global.common.response.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@Tag(name = "댓글 API", description = "게시글 댓글 관련 API")
public class CommentController {

    private final CommentService commentService;
    private final CommentLikeService commentLikeService;


    /**
     * 댓글 작성 API
     */
    @PostMapping("/posts/{postId}/comments")
    @PreAuthorize("isAuthenticated() &&  @accessChecker.canAccessOnComments(#postId, authentication.principal)")
    @Operation(
            summary = "댓글 작성",
            description = "게시글에 댓글을 작성합니다. parentId가 없으면 일반 댓글, 있으면 대댓글로 등록됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "댓글 작성 성공",
                    content = @Content(schema = @Schema(implementation = CommentResponseDto.CreateCommentResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    public ResponseEntity<CustomResponse<CommentResponseDto.CreateCommentResponse>> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequestDto.CreateCommentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getId());
        CommentResponseDto.CreateCommentResponse response = commentService.createComment(memberId, postId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CustomResponse.success("댓글이 작성되었습니다.", response));
    }

    /**
     * 댓글 상세 조회 API
     */
    @GetMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "댓글 상세 조회",
            description = "특정 댓글의 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = CommentResponseDto.CommentDetailResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    public ResponseEntity<CustomResponse<CommentResponseDto.CommentDetailResponse>> getCommentDetail(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getId());
        CommentResponseDto.CommentDetailResponse response = commentService.getCommentDetail(memberId, commentId);
        return ResponseEntity.ok(CustomResponse.success("댓글을 성공적으로 불러왔습니다.", response));
    }

    /**
     * 댓글 삭제 API
     */
    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("@accessChecker.isCommentOwner(#commentId, authentication.principal)")
    @Operation(
            summary = "댓글 삭제",
            description = "댓글을 삭제합니다. 자신이 작성한 댓글만 삭제할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 삭제 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "댓글 없음")
    })
    public ResponseEntity<CustomResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getId());
        commentService.deleteComment(memberId, commentId);
        return ResponseEntity.ok(CustomResponse.success("댓글이 삭제되었습니다.", null));
    }

    /**
     * 게시글의 댓글 목록 조회 API
     */
    @GetMapping("/posts/{postId}/comments")
    @PreAuthorize("isAuthenticated() && @accessChecker.canAccessOnComments(#postId, authentication.principal)")
    @Operation(
            summary = "게시글의 댓글 목록 조회",
            description = "게시글에 작성된 댓글 목록을 조회합니다. 페이지네이션을 지원합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = CommentResponseDto.CommentListResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    public ResponseEntity<CustomResponse<CommentResponseDto.CommentListResponse>> getCommentsByPostId(
            @PathVariable Long postId,
            @Parameter(description = "한 번에 불러올 댓글 수 (기본값: 12)") @RequestParam(required = false) Integer limit,
            @Parameter(description = "페이지네이션 커서 (이전 응답의 next_cursor)") @RequestParam(required = false) Long cursor,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getId());
        CommentRequestDto.CommentPageRequest pageRequest = new CommentRequestDto.CommentPageRequest(limit, cursor);
        CommentResponseDto.CommentListResponse response = commentService.getCommentsByPostId(memberId, postId, pageRequest);
        return ResponseEntity.ok(CustomResponse.success("댓글 목록을 조회했습니다.", response));
    }

    /**
     * 댓글의 대댓글 목록 조회 API
     */
    @GetMapping("/comments/{commentId}/recomments")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "댓글의 대댓글 목록 조회",
            description = "특정 댓글에 작성된 대댓글 목록을 조회합니다. 페이지네이션을 지원합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대댓글 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = CommentResponseDto.RecommentListResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "댓글 없음")
    })
    public ResponseEntity<CustomResponse<CommentResponseDto.RecommentListResponse>> getRecommentsByCommentId(
            @PathVariable Long commentId,
            @Parameter(description = "한 번에 불러올 대댓글 수 (기본값: 12)") @RequestParam(required = false) Integer limit,
            @Parameter(description = "페이지네이션 커서 (이전 응답의 next_cursor)") @RequestParam(required = false) Long cursor,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getId());
        CommentRequestDto.RecommentPageRequest pageRequest = new CommentRequestDto.RecommentPageRequest(limit, cursor);
        CommentResponseDto.RecommentListResponse response = commentService.getRecommentsByCommentId(memberId, commentId, pageRequest);
        return ResponseEntity.ok(CustomResponse.success("대댓글 목록을 조회했습니다.", response));
    }


    /**
     * 댓글 좋아요 추가 API
     */
    @PostMapping("/comments/{commentId}/likes")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "댓글 좋아요 추가",
            description = "댓글에 좋아요를 추가합니다. 이미 좋아요를 누른 경우 에러가 발생합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "좋아요 추가 성공",
                    content = @Content(schema = @Schema(implementation = CommentResponseDto.CommentLikeResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 좋아요한 댓글입니다")
    })
    public ResponseEntity<CustomResponse<CommentResponseDto.CommentLikeResponse>> addCommentLike(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getId());
        CommentResponseDto.CommentLikeResponse response = commentLikeService.addCommentLike(memberId, commentId);
        return ResponseEntity.ok(CustomResponse.success("댓글에 좋아요를 눌렀습니다.", response));
    }

    /**
     * 댓글 좋아요 취소 API
     */
    @DeleteMapping("/comments/{commentId}/likes")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "댓글 좋아요 취소",
            description = "댓글의 좋아요를 취소합니다. 좋아요하지 않은 경우 에러가 발생합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "좋아요 취소 성공",
                    content = @Content(schema = @Schema(implementation = CommentResponseDto.CommentLikeResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "좋아요하지 않은 댓글입니다")
    })
    public ResponseEntity<CustomResponse<CommentResponseDto.CommentLikeResponse>> removeCommentLike(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getId());
        CommentResponseDto.CommentLikeResponse response = commentLikeService.removeCommentLike(memberId, commentId);
        return ResponseEntity.ok(CustomResponse.success("댓글 좋아요를 취소했습니다.", response));
    }

    /**
     * 대댓글 좋아요 추가 API
     */
    @PostMapping("/recomments/{recommentId}/likes")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "대댓글 좋아요 추가",
            description = "대댓글에 좋아요를 추가합니다. 이미 좋아요를 누른 경우 에러가 발생합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "좋아요 추가 성공",
                    content = @Content(schema = @Schema(implementation = CommentResponseDto.RecommentLikeResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "대댓글을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 좋아요한 대댓글입니다")
    })
    public ResponseEntity<CustomResponse<CommentResponseDto.RecommentLikeResponse>> addRecommentLike(
            @PathVariable Long recommentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getId());
        CommentResponseDto.RecommentLikeResponse response = commentLikeService.addRecommentLike(memberId, recommentId);
        return ResponseEntity.ok(CustomResponse.success("대댓글에 좋아요를 눌렀습니다.", response));
    }

    /**
     * 대댓글 좋아요 취소 API
     */
    @DeleteMapping("/recomments/{recommentId}/likes")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "대댓글 좋아요 취소",
            description = "대댓글의 좋아요를 취소합니다. 좋아요하지 않은 경우 에러가 발생합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "좋아요 취소 성공",
                    content = @Content(schema = @Schema(implementation = CommentResponseDto.RecommentLikeResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "대댓글을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "좋아요하지 않은 대댓글입니다")
    })
    public ResponseEntity<CustomResponse<CommentResponseDto.RecommentLikeResponse>> removeRecommentLike(
            @PathVariable Long recommentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getId());
        CommentResponseDto.RecommentLikeResponse response = commentLikeService.removeRecommentLike(memberId, recommentId);
        return ResponseEntity.ok(CustomResponse.success("대댓글 좋아요를 취소했습니다.", response));
    }


    /**
     * 대댓글 삭제 API
     */
    @DeleteMapping("/recomments/{recommentId}")
    @PreAuthorize("@accessChecker.isRecommentOwner(#recommentId, authentication.principal)")
    @Operation(
            summary = "대댓글 삭제",
            description = "대댓글을 삭제합니다. 자신이 작성한 대댓글만 삭제할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대댓글 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "본인이 작성한 대댓글만 삭제할 수 있습니다"),
            @ApiResponse(responseCode = "404", description = "해당 대댓글을 찾을 수 없습니다")
    })
    public ResponseEntity<CustomResponse<Void>> deleteRecomment(
            @PathVariable Long recommentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getId());
        commentService.deleteRecomment(memberId, recommentId);
        return ResponseEntity.ok(CustomResponse.success("대댓글이 삭제되었습니다.", null));
    }
}