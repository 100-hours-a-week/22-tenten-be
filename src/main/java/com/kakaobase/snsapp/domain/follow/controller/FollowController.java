package com.kakaobase.snsapp.domain.follow.controller;


import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.comments.dto.CommentResponseDto;
import com.kakaobase.snsapp.domain.follow.dto.FollowResponse;
import com.kakaobase.snsapp.domain.follow.service.FollowService;
import com.kakaobase.snsapp.global.common.response.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "팔로우 API", description = "팔로잉, 팔로우 관련 API")
public class FollowController {

    private final FollowService followService;

    @PostMapping("{targetUserId}/follows")
    @Operation(
            summary = "팔로우 요청",
            description = "유저에게 팔로우를 요청합니다"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "팔로우 요청 성공",
                    content = @Content(schema = @Schema(implementation = CommentResponseDto.CreateCommentResponse.class))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 요청"),
            @ApiResponse(responseCode = "404", description = "해당 유저를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 팔로우 한 유저")
    })
    public CustomResponse<?> createFollowing(
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        followService.addFollowing(targetUserId, userDetails);

        return CustomResponse.success("팔로우가 성공적으로 완료되었습니다.");
    }

    @DeleteMapping("{targetUserId}/follows")
    @Operation(
            summary = "언팔로우 요청",
            description = "유저에게 팔로잉 취소를 요청합니다"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "팔로우 취소 성공",
                    content = @Content(schema = @Schema(implementation = CommentResponseDto.CreateCommentResponse.class))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 요청"),
            @ApiResponse(responseCode = "404", description = "해당 유저를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 언팔로우 한 유저")
    })
    public CustomResponse<?> deleteFollowing(
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        followService.removeFollowing(targetUserId, userDetails);

        return CustomResponse.success("팔로우를 성공적으로 취소하였습니다.");
    }

    @GetMapping("{userId}/followers")
    @Operation(
            summary = "팔로워 목록 요청",
            description = "특정유저를 팔로잉하는 회원 목록을 요청합니다"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "팔로우 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = CommentResponseDto.CreateCommentResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 요청"),
            @ApiResponse(responseCode = "404", description = "해당 유저를 찾을 수 없음")
    })
    public CustomResponse<List<FollowResponse.UserInfo>> getFollowerList(
            @PathVariable Long userId,
            @Parameter(description = "한 번에 불러올 팔로워 수 (기본값: 22)") @RequestParam(required = false, defaultValue = "22") Integer limit,
            @Parameter(description = "페이지네이션 커서 (이전 응답의 next_cursor)") @RequestParam(required = false) Long cursor
    ) {
            List<FollowResponse.UserInfo> response =followService.getFollowers(userId, limit, cursor);

            return CustomResponse.success("팔로워 목록이 정상적으로 조회되었습니다" , response);
    }

    @GetMapping("{userId}/followings")
    @Operation(
            summary = "팔로잉 목록 요청",
            description = "특정유저를 팔로잉하는 회원 목록을 요청합니다"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "팔로우 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = CommentResponseDto.CreateCommentResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 요청"),
            @ApiResponse(responseCode = "404", description = "해당 유저를 찾을 수 없음")
    })
    public CustomResponse<List<FollowResponse.UserInfo>> getFollowingList(
            @PathVariable Long userId,
            @Parameter(description = "한 번에 불러올 팔로워 수 (기본값: 22)") @RequestParam(required = false, defaultValue = "22") Integer limit,
            @Parameter(description = "페이지네이션 커서 (이전 응답의 next_cursor)") @RequestParam(required = false) Long cursor
    ) {
        List<FollowResponse.UserInfo> response =followService.getFollowings(userId, limit, cursor);

        return CustomResponse.success("팔로잉 목록이 정상적으로 조회되었습니다" , response);
    }
}
