package com.kakaobase.snsapp.domain.comments.controller;

import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.comments.dto.CommentRequestDto;
import com.kakaobase.snsapp.domain.comments.dto.CommentResponseDto;
import com.kakaobase.snsapp.domain.comments.service.CommentService;
import com.kakaobase.snsapp.domain.comments.service.CommentLikeService;
import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.global.common.response.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
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
    @Operation(
            summary = "댓글 상세 조회",
            description = "특정 댓글의 상세 정보를 조회합니다."
    )
    public CustomResponse<CommentResponseDto.CommentInfo> getCommentDetail(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getId());
        CommentResponseDto.CommentInfo response = commentService.getCommentInfo(memberId, commentId);
        return CustomResponse.success("댓글을 성공적으로 불러왔습니다.", response);
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
    public ResponseEntity<CustomResponse<Void>> deleteComment(
            @PathVariable Long commentId
    ) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok(CustomResponse.success("댓글이 삭제되었습니다.", null));
    }

    /**
     * 게시글의 댓글 목록 조회 API
     */
    @GetMapping("/posts/{postId}/comments")
    @Operation(
            summary = "게시글의 댓글 목록 조회",
            description = "게시글에 작성된 댓글 목록을 조회합니다. 페이지네이션을 지원합니다."
    )
    public CustomResponse<List<CommentResponseDto.CommentInfo>> getCommentsByPostId(
            @PathVariable Long postId,
            @Parameter(description = "한 번에 불러올 댓글 수 (기본값: 12)") @RequestParam(required = false, defaultValue = "12") Integer limit,
            @Parameter(description = "페이지네이션 커서 (이전 응답의 next_cursor)") @RequestParam(required = false) Long cursor,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getId());
        List<CommentResponseDto.CommentInfo> response = commentService.getCommentsByPostId(memberId, postId, limit, cursor);
        return CustomResponse.success("댓글 목록을 조회했습니다.", response);
    }

    /**
     * 댓글의 대댓글 목록 조회 API
     */
    @GetMapping("/comments/{commentId}/recomments")
    @Operation(
            summary = "댓글의 대댓글 목록 조회",
            description = "특정 댓글에 작성된 대댓글 목록을 조회합니다. 페이지네이션을 지원합니다."
    )
    public CustomResponse<List<CommentResponseDto.RecommentInfo>> getRecommentsByCommentId(
            @PathVariable Long commentId,
            @Parameter(description = "한 번에 불러올 대댓글 수 (기본값: 12)") @RequestParam(required = false, defaultValue = "12") Integer limit,
            @Parameter(description = "페이지네이션 커서 (이전 응답의 next_cursor)") @RequestParam(required = false) Long cursor,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getId());
        List<CommentResponseDto.RecommentInfo> response = commentService.getRecommentInfoList(memberId, commentId, limit, cursor);
        return CustomResponse.success("대댓글 목록을 조회했습니다.", response);
    }

    /**
     * 댓글 좋아요 추가 API
     */
    @PostMapping("/comments/{commentId}/likes")
    @Operation(
            summary = "댓글 좋아요 추가",
            description = "댓글에 좋아요를 추가합니다. 이미 좋아요를 누른 경우 에러가 발생합니다."
    )
    public CustomResponse<Void> addCommentLike(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getId());commentLikeService.addCommentLike(memberId, commentId);
        return CustomResponse.success("좋아요가 성공적으로 등록되었습니다");
    }

    /**
     * 댓글 좋아요 취소 API
     */
    @DeleteMapping("/comments/{commentId}/likes")
    @Operation(
            summary = "댓글 좋아요 취소",
            description = "댓글의 좋아요를 취소합니다. 좋아요하지 않은 경우 에러가 발생합니다."
    )
    public CustomResponse<Void> removeCommentLike(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getId());
        commentLikeService.removeCommentLike(memberId, commentId);
        return CustomResponse.success("좋아요가 성공적으로 취소되었습니다.");
    }

    /**
     * 대댓글 좋아요 추가 API
     */
    @PostMapping("/recomments/{recommentId}/likes")
    @Operation(
            summary = "대댓글 좋아요 추가",
            description = "대댓글에 좋아요를 추가합니다. 이미 좋아요를 누른 경우 에러가 발생합니다."
    )
    public CustomResponse<Void> addRecommentLike(
            @PathVariable Long recommentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getId());
        commentLikeService.addRecommentLike(memberId, recommentId);
        return CustomResponse.success("좋아요가 성공적으로 등록되었습니다");
    }

    /**
     * 대댓글 좋아요 취소 API
     */
    @DeleteMapping("/recomments/{recommentId}/likes")
    @Operation(
            summary = "대댓글 좋아요 취소",
            description = "대댓글의 좋아요를 취소합니다. 좋아요하지 않은 경우 에러가 발생합니다."
    )
    public CustomResponse<Void> removeRecommentLike(
            @PathVariable Long recommentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getId());
        commentLikeService.removeRecommentLike(memberId, recommentId);
        return CustomResponse.success("대댓글 좋아요를 취소했습니다.");
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
    public CustomResponse<Void> deleteRecomment(
            @PathVariable Long recommentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getId());
        commentService.deleteRecomment(memberId, recommentId);
        return CustomResponse.success("대댓글이 삭제되었습니다.", null);
    }

    /**
     * 댓글에 좋아요를 누른 회원 목록 조회
     */
    @GetMapping("comments/{commentId}/likes")
    @Operation(summary = "댓글 좋아요 유저 목록 조회", description = "댓글에 좋아요를 누른 유저를 조회합니다.")
    public CustomResponse<List<MemberResponseDto.UserInfo>> getCommentLikedMemberList(
            @Parameter(description = "댓글 ID") @PathVariable Long commentId,
            @Parameter(description = "한 페이지에 표시할 유저 수") @RequestParam(defaultValue = "12") int limit,
            @Parameter(description = "마지막으로 조회한 유저 ID") @RequestParam(required = false) Long cursor
    ) {

        List<MemberResponseDto.UserInfo> response= commentLikeService.getCommentLikedMembers(commentId, limit, cursor);

        return CustomResponse.success("좋아요 유저 목록을 성공적으로 불러왔습니다", response);
    }

    /**
     * 대댓글에 좋아요를 누른 회원 목록 조회
     */
    @GetMapping("/recomments/{recommentId}/likes")
    @Operation(summary = "대댓글 좋아요 유저 목록 조회", description = "대댓글에 좋아요를 누른 유저를 조회합니다.")
    public CustomResponse<List<MemberResponseDto.UserInfo>> getRecommentLikedMemberList(
            @Parameter(description = "댓글 ID") @PathVariable Long recommentId,
            @Parameter(description = "한 페이지에 표시할 유저 수") @RequestParam(defaultValue = "12") int limit,
            @Parameter(description = "마지막으로 조회한 유저 ID") @RequestParam(required = false) Long cursor
    ) {

        List<MemberResponseDto.UserInfo> response= commentLikeService.getRecommentLikedMembers(recommentId, limit, cursor);

        return CustomResponse.success("좋아요 유저 목록을 성공적으로 불러왔습니다", response);
    }
}