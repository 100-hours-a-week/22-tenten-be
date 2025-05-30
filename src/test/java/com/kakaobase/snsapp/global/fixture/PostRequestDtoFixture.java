package com.kakaobase.snsapp.global.fixture;

import com.kakaobase.snsapp.domain.posts.dto.PostRequestDto.*;

import static com.kakaobase.snsapp.global.constants.PostFixtureConstants.*;
import static com.kakaobase.snsapp.global.constants.PostImageFixtureConstants.*;

public class PostRequestDtoFixture {

    // ========== PostCreateRequestDto 생성 메서드 ==========

    /**
     * 기본 게시글 생성 요청 DTO를 생성합니다.
     */
    public static PostCreateRequestDto createBasicPostCreateRequest() {
        return new PostCreateRequestDto(
                VALID_CONTENT,
                VALID_IMAGE_URL_1,
                VALID_YOUTUBE_URL
        );
    }

    /**
     * 내용만 있는 게시글 생성 요청 DTO를 생성합니다.
     */
    public static PostCreateRequestDto createContentOnlyRequest() {
        return new PostCreateRequestDto(
                VALID_CONTENT,
                null,
                null
        );
    }

    /**
     * 이미지만 있는 게시글 생성 요청 DTO를 생성합니다.
     */
    public static PostCreateRequestDto createImageOnlyRequest() {
        return new PostCreateRequestDto(
                null,
                VALID_IMAGE_URL_1,
                null
        );
    }

    /**
     * 유튜브 URL만 있는 게시글 생성 요청 DTO를 생성합니다.
     */
    public static PostCreateRequestDto createYoutubeOnlyRequest() {
        return new PostCreateRequestDto(
                null,
                null,
                VALID_YOUTUBE_URL
        );
    }

    /**
     * 긴 내용의 게시글 생성 요청 DTO를 생성합니다.
     */
    public static PostCreateRequestDto createLongContentRequest() {
        return new PostCreateRequestDto(
                LONG_VALID_CONTENT,
                null,
                null
        );
    }

    // ========== 유효성 검증 실패용 PostCreateRequestDto ==========

    /**
     * 모든 내용이 비어있는 게시글 생성 요청 DTO를 생성합니다.
     */
    public static PostCreateRequestDto createEmptyRequest() {
        return new PostCreateRequestDto(
                null,
                null,
                null
        );
    }

    /**
     * 빈 문자열로만 구성된 게시글 생성 요청 DTO를 생성합니다.
     */
    public static PostCreateRequestDto createBlankRequest() {
        return new PostCreateRequestDto(
                BLANK_CONTENT,
                "",
                ""
        );
    }

    /**
     * 공백만 있는 게시글 생성 요청 DTO를 생성합니다.
     */
    public static PostCreateRequestDto createWhitespaceRequest() {
        return new PostCreateRequestDto(
                WHITESPACE_CONTENT,
                "   ",
                "   "
        );
    }

    /**
     * 너무 긴 내용의 게시글 생성 요청 DTO를 생성합니다. (2000자 초과)
     */
    public static PostCreateRequestDto createTooLongContentRequest() {
        return new PostCreateRequestDto(
                TOO_LONG_CONTENT,
                null,
                null
        );
    }

    /**
     * 잘못된 유튜브 URL의 게시글 생성 요청 DTO를 생성합니다.
     */
    public static PostCreateRequestDto createInvalidYoutubeRequest() {
        return new PostCreateRequestDto(
                VALID_CONTENT,
                null,
                INVALID_YOUTUBE_URL
        );
    }

    /**
     * 형식이 잘못된 유튜브 URL의 게시글 생성 요청 DTO를 생성합니다.
     */
    public static PostCreateRequestDto createMalformedYoutubeRequest() {
        return new PostCreateRequestDto(
                VALID_CONTENT,
                null,
                MALFORMED_YOUTUBE_URL
        );
    }

    // ========== PostSearchRequestDto 생성 메서드 ==========

    /**
     * 기본 게시글 검색 요청 DTO를 생성합니다.
     */
    public static PostSearchRequestDto createBasicSearchRequest() {
        return new PostSearchRequestDto(
                DEFAULT_LIMIT,
                null,
                null
        );
    }

    /**
     * 커스텀 limit의 게시글 검색 요청 DTO를 생성합니다.
     */
    public static PostSearchRequestDto createCustomLimitSearchRequest() {
        return new PostSearchRequestDto(
                CUSTOM_LIMIT,
                null,
                null
        );
    }

    /**
     * 커서가 포함된 게시글 검색 요청 DTO를 생성합니다.
     */
    public static PostSearchRequestDto createCursorSearchRequest() {
        return new PostSearchRequestDto(
                DEFAULT_LIMIT,
                CURSOR_POST_ID,
                "2024-04-23T10:00:00Z"
        );
    }

    /**
     * 모든 파라미터가 null인 게시글 검색 요청 DTO를 생성합니다.
     */
    public static PostSearchRequestDto createNullSearchRequest() {
        return new PostSearchRequestDto(
                null,
                null,
                null
        );
    }

    // ========== ImageUrlRequestDto 생성 메서드 ==========

    /**
     * 기본 이미지 URL 요청 DTO를 생성합니다.
     */
    public static ImageUrlRequestDto createBasicImageUrlRequest() {
        return new ImageUrlRequestDto(
                "test-image.jpg",
                TYPICAL_IMAGE_SIZE_BYTES,
                "image/jpeg"
        );
    }

    /**
     * PNG 이미지 URL 요청 DTO를 생성합니다.
     */
    public static ImageUrlRequestDto createPngImageUrlRequest() {
        return new ImageUrlRequestDto(
                "test-image.png",
                TYPICAL_IMAGE_SIZE_BYTES,
                "image/png"
        );
    }

    /**
     * GIF 이미지 URL 요청 DTO를 생성합니다.
     */
    public static ImageUrlRequestDto createGifImageUrlRequest() {
        return new ImageUrlRequestDto(
                "test-image.gif",
                TYPICAL_IMAGE_SIZE_BYTES,
                "image/gif"
        );
    }

    /**
     * 큰 크기의 이미지 URL 요청 DTO를 생성합니다.
     */
    public static ImageUrlRequestDto createLargeSizeImageUrlRequest() {
        return new ImageUrlRequestDto(
                "large-image.jpg",
                MAX_IMAGE_SIZE_BYTES,
                "image/jpeg"
        );
    }

    // ========== 유효성 검증 실패용 ImageUrlRequestDto ==========

    /**
     * 빈 파일명의 이미지 URL 요청 DTO를 생성합니다.
     */
    public static ImageUrlRequestDto createBlankFilenameRequest() {
        return new ImageUrlRequestDto(
                "",
                TYPICAL_IMAGE_SIZE_BYTES,
                "image/jpeg"
        );
    }

    /**
     * null 파일명의 이미지 URL 요청 DTO를 생성합니다.
     */
    public static ImageUrlRequestDto createNullFilenameRequest() {
        return new ImageUrlRequestDto(
                null,
                TYPICAL_IMAGE_SIZE_BYTES,
                "image/jpeg"
        );
    }

    /**
     * 빈 MIME 타입의 이미지 URL 요청 DTO를 생성합니다.
     */
    public static ImageUrlRequestDto createBlankMimeTypeRequest() {
        return new ImageUrlRequestDto(
                "test-image.jpg",
                TYPICAL_IMAGE_SIZE_BYTES,
                ""
        );
    }

    /**
     * null MIME 타입의 이미지 URL 요청 DTO를 생성합니다.
     */
    public static ImageUrlRequestDto createNullMimeTypeRequest() {
        return new ImageUrlRequestDto(
                "test-image.jpg",
                TYPICAL_IMAGE_SIZE_BYTES,
                null
        );
    }

    // ========== YouTubeAiRequest 생성 메서드 ==========

    /**
     * 기본 YouTube AI 요청 DTO를 생성합니다.
     */
    public static YouTubeAiRequest createBasicYouTubeAiRequest() {
        return new YouTubeAiRequest(VALID_YOUTUBE_URL);
    }

    /**
     * 잘못된 URL의 YouTube AI 요청 DTO를 생성합니다.
     */
    public static YouTubeAiRequest createInvalidYouTubeAiRequest() {
        return new YouTubeAiRequest(INVALID_YOUTUBE_URL);
    }

    /**
     * null URL의 YouTube AI 요청 DTO를 생성합니다.
     */
    public static YouTubeAiRequest createNullUrlYouTubeAiRequest() {
        return new YouTubeAiRequest(null);
    }

    // ========== 조합 테스트용 메서드 ==========

    /**
     * 내용과 이미지가 모두 있는 게시글 생성 요청 DTO를 생성합니다.
     */
    public static PostCreateRequestDto createContentWithImageRequest() {
        return new PostCreateRequestDto(
                VALID_CONTENT,
                VALID_IMAGE_URL_1,
                null
        );
    }

    /**
     * 내용과 유튜브 URL이 모두 있는 게시글 생성 요청 DTO를 생성합니다.
     */
    public static PostCreateRequestDto createContentWithYoutubeRequest() {
        return new PostCreateRequestDto(
                VALID_CONTENT,
                null,
                VALID_YOUTUBE_URL
        );
    }

    /**
     * 이미지와 유튜브 URL이 모두 있는 게시글 생성 요청 DTO를 생성합니다.
     */
    public static PostCreateRequestDto createImageWithYoutubeRequest() {
        return new PostCreateRequestDto(
                null,
                VALID_IMAGE_URL_1,
                VALID_YOUTUBE_URL
        );
    }

    private PostRequestDtoFixture() {
        // 유틸리티 클래스 - 인스턴스화 방지
    }
}