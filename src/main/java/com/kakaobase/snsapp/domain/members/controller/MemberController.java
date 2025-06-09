package com.kakaobase.snsapp.domain.members.controller;

import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.comments.dto.CommentResponseDto;
import com.kakaobase.snsapp.domain.comments.service.CommentService;
import com.kakaobase.snsapp.domain.members.dto.MemberRequestDto;
import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.members.service.MemberService;
import com.kakaobase.snsapp.domain.posts.dto.PostResponseDto;
import com.kakaobase.snsapp.domain.posts.service.PostService;
import com.kakaobase.snsapp.global.common.response.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 회원 관련 API 컨트롤러
 */
@Slf4j
@Tag(name = "회원 API", description = "회원 관련 API")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final PostService postService;
    private final CommentService commentService;

    @Operation(summary = "회원가입", description = "새로운 회원을 등록합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "회원가입 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 입력값"),
            @ApiResponse(responseCode = "401", description = "이메일 인증 미완료"),
            @ApiResponse(responseCode = "409", description = "이미 등록된 이메일 또는 닉네임")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomResponse<Void> postUser(
            @Parameter(description = "회원가입 정보", required = true)
            @Valid @RequestBody MemberRequestDto.SignUp request) {

        memberService.signUp(request);
        return CustomResponse.success("회원가입이 완료되었습니다.");
    }

    @Operation(summary = "유저 마이페이지 조회", description = "특정 회원의 마이페이지에 필요한 정보를 반환합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "유저 마이페이지 조회에 성공하였습니다",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class))),
            @ApiResponse(responseCode = "401", description = "로그인 되지 않음"),
            @ApiResponse(responseCode = "404", description = "해당 사용자를 찾을 수 없음")
    })

    @GetMapping("/{userId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CustomResponse<MemberResponseDto.Mypage> getUserMyPage(
            @Parameter(description = "조회할 유저의 id", required = true)
            @PathVariable Long userId
    ) {
        MemberResponseDto.Mypage response = memberService.getMypageInfo(userId);

        return CustomResponse.success("유저 마이페이지 조회에 성공하였습니다", response);
    }

    @GetMapping("/{userId}/posts")
    @Operation(summary = "유저가 작성한 게시글 목록 조회", description = "유저가 작성한 게시글 목록을 조회합니다.")
    public CustomResponse<List<PostResponseDto.PostDetails>> getUserPosts(
            @Parameter(description = "한 페이지에 표시할 게시글 수") @RequestParam(defaultValue = "12") int limit,
            @Parameter(description = "마지막으로 조회한 게시글 ID") @RequestParam(required = false) Long cursor,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        Long memberId = Long.valueOf(userDetails.getId());

        List<PostResponseDto.PostDetails> response = postService.getUserPostList(limit, cursor, memberId);

        return CustomResponse.success("유저 게시글이조회에 성공하였습니다",response);
    }

    @GetMapping("/{userId}/comments")
    @Operation(summary = "유저가 작성한 댓글 목록 조회", description = "유저가 작성한 댓글 목록을 조회합니다.")
    public CustomResponse<List<CommentResponseDto.CommentInfo>> getUserComments(
            @Parameter(description = "한 페이지에 표시할 댓글 수") @RequestParam(defaultValue = "12") int limit,
            @Parameter(description = "마지막으로 조회한 댓글 ID") @RequestParam(required = false) Long cursor,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        Long memberId = Long.valueOf(userDetails.getId());

        List<CommentResponseDto.CommentInfo> response = commentService.getUserCommentList(limit, cursor, memberId);

        return CustomResponse.success("유저 댓글 조회에 성공하였습니다",response);
    }

    @GetMapping("/{userId}/liked-posts")
    @Operation(summary = "유저가 좋아요한 게시글 목록 조회", description = "유저가 좋아요한 게시글 목록을 조회합니다.")
    public CustomResponse<List<PostResponseDto.PostDetails>> getLikedPosts(
            @Parameter(description = "한 페이지에 표시할 게시글 수") @RequestParam(defaultValue = "12") int limit,
            @Parameter(description = "마지막으로 조회한 게시글 ID") @RequestParam(required = false) Long cursor,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        Long memberId = Long.valueOf(userDetails.getId());

        List<PostResponseDto.PostDetails> response = postService.getLikedPostList(limit, cursor, memberId);

        return CustomResponse.success("좋아요한 게시글 목록이 정상적으로 조회되었습니다",response);
    }

    @Operation(summary = "비밀번호 수정", description = "회원의 비밀번호를 수정합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "비밀번호 수정성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 입력값"),
            @ApiResponse(responseCode = "401", description = "이메일 인증 미완료"),
            @ApiResponse(responseCode = "401", description = "로그인 되지 않음")
    })
    @PutMapping("/password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("isAuthenticated()")
    public CustomResponse<Void> putPassword(
            @Parameter(description = "비밀번호 수정 요청", required = true)
            @Valid @RequestBody MemberRequestDto.PasswordChange request
    ) {
        memberService.changePassword(request);
        return CustomResponse.success("비밀번호 수정이 완료되었습니다");
    }

    @Operation(summary = "GithubUrl 수정", description = "회원의 GithubUrl를 수정합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "GithubUrl 수정성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 입력값"),
            @ApiResponse(responseCode = "401", description = "로그인 되지 않음")
    })
    @PutMapping("/github-url")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CustomResponse<Void> putGithubUrl(
            @Parameter(description = "GithubUrl 수정 요청", required = true)
            @Valid @RequestBody MemberRequestDto.GithubUrlChange request
    ) {
        memberService.changGithubUrl(request);
        return CustomResponse.success("GitHub 링크가 성공적으로 변경되었습니다.");
    }

    @Operation(summary = "프로필 이미지 수정", description = "회원의 프로필 이미지를 수정합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로필 이미지 수정성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 입력값"),
            @ApiResponse(responseCode = "401", description = "로그인 되지 않음")
    })
    @PutMapping("/images")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CustomResponse<MemberResponseDto.ProfileImageChange> putProfileImageUrl(
            @Parameter(description = "프로필 이미지 수정 요청", required = true)
            @Valid @RequestBody MemberRequestDto.ProfileImageChange request
    ) {
        MemberResponseDto.ProfileImageChange newImageUrl = memberService.changProfileImageUrl(request);
        return CustomResponse.success("프로필 이미지가 성공적으로 변경되었습니다.", newImageUrl);
    }


    @Operation(summary = "회원탈퇴", description = "기존 회원을 탈퇴시킵니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원탈퇴 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 입력값"),
            @ApiResponse(responseCode = "401", description = "이메일 인증 미완료"),
            @ApiResponse(responseCode = "401", description = "로그인 되지 않음")
    })
    @DeleteMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CustomResponse<Void> deleteUser() {

        memberService.unregister();
        return CustomResponse.success("회원탈퇴가 완료되었습니다.");
    }


}