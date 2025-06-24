package com.kakaobase.snsapp.global.common.s3.controller;

import com.kakaobase.snsapp.domain.posts.entity.Post;
import com.kakaobase.snsapp.global.common.response.CustomResponse;
import com.kakaobase.snsapp.global.common.s3.service.S3Service;
import com.kakaobase.snsapp.global.common.s3.dto.PresignedUrlResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * S3 이미지 관련 API를 제공하는 컨트롤러
 *
 * <p>이 컨트롤러는 S3에 이미지를 업로드하기 위한 Presigned URL 발급 등
 * S3 관련 API를 제공합니다.</p>
 */
@Tag(name = "이미지 관리", description = "이미지 업로드를 위한 Presigned URL 발급 API")
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class S3Controller {

    private final S3Service s3Service;

    /**
     * S3에 이미지를 업로드하기 위한 Presigned URL을 발급합니다.
     *
     * @param fileName 업로드할 파일명
     * @param fileSize 파일 크기 (바이트 단위)
     * @param mimeType 파일의 MIME 타입 (예: image/jpeg)
     * @param type 이미지 사용 용도 (예: profile_image)
     * @return Presigned URL 정보가 포함된 응답
     */
    @Operation(
            summary = "Presigned URL 발급",
            description = "S3에 이미지를 업로드할 수 있는 Presigned URL을 발급합니다. 발급된 URL은 제한된 시간 동안만 유효합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Presigned URL 발급 성공",
                    content = @Content(schema = @Schema(implementation = PresignedUrlResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 파라미터 (지원하지 않는 이미지 형식 또는 파일 크기 초과)",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 오류 (토큰 없음 또는 유효하지 않은 토큰)",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class))
            )
    })
    @GetMapping("/presigned-url")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CustomResponse<PresignedUrlResponseDto>> getPresignedUrl(
            @RequestParam @NotBlank(message = "파일명은 필수입니다") String fileName,
            @RequestParam @Positive(message = "파일 크기는 0보다 커야 합니다") Long fileSize,
            @RequestParam @NotBlank(message = "MIME 타입은 필수입니다") String mimeType,
            @RequestParam @NotBlank(message = "이미지 타입은 필수입니다") String type,
            @RequestParam(required = false) Post.BoardType boardType
    ) {
        // Presigned URL 생성 및 반환
        PresignedUrlResponseDto response = s3Service.generatePresignedUrl(fileName, fileSize, mimeType, type);

        return ResponseEntity.ok(
                CustomResponse.success("S3에 이미지를 업로드할 수 있도록 presigned URL을 발급했습니다.", response)
        );
    }
}