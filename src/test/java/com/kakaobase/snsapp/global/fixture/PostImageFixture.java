package com.kakaobase.snsapp.global.fixture;

import com.kakaobase.snsapp.domain.posts.entity.Post;
import com.kakaobase.snsapp.domain.posts.entity.PostImage;

import java.util.ArrayList;
import java.util.List;

import static com.kakaobase.snsapp.global.constants.PostImageFixtureConstants.*;

public class PostImageFixture {

    // ========== 기본 PostImage 생성 메서드 ==========

    /**
     * 기본 PostImage를 생성합니다.
     */
    public static PostImage createBasicPostImage(Post post) {
        return PostImage.builder()
                .post(post)
                .sortIndex(POST_IMAGE_SORT_INDEX)
                .imgUrl(POST_IMAGE_URL)
                .build();
    }

    /**
     * 특정 정렬 순서와 URL의 PostImage를 생성합니다.
     */
    public static PostImage createPostImage(Post post, Integer sortIndex, String imgUrl) {
        return PostImage.builder()
                .post(post)
                .sortIndex(sortIndex)
                .imgUrl(imgUrl)
                .build();
    }

    /**
     * 첫 번째 이미지를 생성합니다.
     */
    public static PostImage createFirstPostImage(Post post) {
        return PostImage.builder()
                .post(post)
                .sortIndex(FIRST_IMAGE_SORT_INDEX)
                .imgUrl(VALID_IMAGE_URL_1)
                .build();
    }

    /**
     * 두 번째 이미지를 생성합니다.
     */
    public static PostImage createSecondPostImage(Post post) {
        return PostImage.builder()
                .post(post)
                .sortIndex(SECOND_IMAGE_SORT_INDEX)
                .imgUrl(VALID_IMAGE_URL_2)
                .build();
    }

    /**
     * 세 번째 이미지를 생성합니다.
     */
    public static PostImage createThirdPostImage(Post post) {
        return PostImage.builder()
                .post(post)
                .sortIndex(THIRD_IMAGE_SORT_INDEX)
                .imgUrl(VALID_IMAGE_URL_3)
                .build();
    }

    // ========== 다중 PostImage 생성 메서드 ==========

    /**
     * 기본적인 여러 개의 PostImage를 생성합니다.
     */
    public static List<PostImage> createMultiplePostImages(Post post) {
        List<PostImage> images = new ArrayList<>();

        images.add(PostImage.builder()
                .post(post)
                .sortIndex(FIRST_IMAGE_SORT_INDEX)
                .imgUrl(VALID_IMAGE_URL_1)
                .build());

        images.add(PostImage.builder()
                .post(post)
                .sortIndex(SECOND_IMAGE_SORT_INDEX)
                .imgUrl(VALID_IMAGE_URL_2)
                .build());

        images.add(PostImage.builder()
                .post(post)
                .sortIndex(THIRD_IMAGE_SORT_INDEX)
                .imgUrl(VALID_IMAGE_URL_3)
                .build());

        return images;
    }

    /**
     * 최대 개수의 PostImage를 생성합니다.
     */
    public static List<PostImage> createMaxPostImages(Post post) {
        List<PostImage> images = new ArrayList<>();

        String[] imageUrls = {
                VALID_IMAGE_URL_1,
                VALID_IMAGE_URL_2,
                VALID_IMAGE_URL_3,
                VALID_IMAGE_URL_4,
                VALID_IMAGE_URL_5
        };

        for (int i = 0; i < MAX_IMAGE_COUNT; i++) {
            images.add(PostImage.builder()
                    .post(post)
                    .sortIndex(i + 1)
                    .imgUrl(imageUrls[i])
                    .build());
        }

        return images;
    }



    // ========== 파일 확장자별 PostImage 생성 메서드 ==========

    /**
     * JPG 이미지를 생성합니다.
     */
    public static PostImage createJpgPostImage(Post post) {
        return PostImage.builder()
                .post(post)
                .sortIndex(FIRST_IMAGE_SORT_INDEX)
                .imgUrl("https://example.com/image" + JPG_EXTENSION)
                .build();
    }

    /**
     * PNG 이미지를 생성합니다.
     */
    public static PostImage createPngPostImage(Post post) {
        return PostImage.builder()
                .post(post)
                .sortIndex(FIRST_IMAGE_SORT_INDEX)
                .imgUrl("https://example.com/image" + PNG_EXTENSION)
                .build();
    }

    /**
     * GIF 이미지를 생성합니다.
     */
    public static PostImage createGifPostImage(Post post) {
        return PostImage.builder()
                .post(post)
                .sortIndex(FIRST_IMAGE_SORT_INDEX)
                .imgUrl("https://example.com/image" + GIF_EXTENSION)
                .build();
    }

    /**
     * WEBP 이미지를 생성합니다.
     */
    public static PostImage createWebpPostImage(Post post) {
        return PostImage.builder()
                .post(post)
                .sortIndex(FIRST_IMAGE_SORT_INDEX)
                .imgUrl("https://example.com/image" + WEBP_EXTENSION)
                .build();
    }

    /**
     * 다양한 확장자의 이미지들을 생성합니다.
     */
    public static List<PostImage> createVariousExtensionImages(Post post) {
        List<PostImage> images = new ArrayList<>();

        for (int i = 0; i < VALID_IMAGE_EXTENSIONS.length; i++) {
            images.add(PostImage.builder()
                    .post(post)
                    .sortIndex(i + 1)
                    .imgUrl("https://example.com/image" + (i + 1) + VALID_IMAGE_EXTENSIONS[i])
                    .build());
        }

        return images;
    }

    // ========== 유효성 검증 실패용 PostImage 생성 메서드 ==========

    /**
     * 잘못된 URL의 PostImage를 생성합니다.
     */
    public static PostImage createInvalidUrlPostImage(Post post) {
        return PostImage.builder()
                .post(post)
                .sortIndex(FIRST_IMAGE_SORT_INDEX)
                .imgUrl(INVALID_IMAGE_URL)
                .build();
    }

    /**
     * 형식이 잘못된 URL의 PostImage를 생성합니다.
     */
    public static PostImage createMalformedUrlPostImage(Post post) {
        return PostImage.builder()
                .post(post)
                .sortIndex(FIRST_IMAGE_SORT_INDEX)
                .imgUrl(MALFORMED_IMAGE_URL)
                .build();
    }

    /**
     * 빈 URL의 PostImage를 생성합니다.
     */
    public static PostImage createBlankUrlPostImage(Post post) {
        return PostImage.builder()
                .post(post)
                .sortIndex(FIRST_IMAGE_SORT_INDEX)
                .imgUrl(BLANK_IMAGE_URL)
                .build();
    }

    /**
     * null URL의 PostImage를 생성합니다.
     */
    public static PostImage createNullUrlPostImage(Post post) {
        return PostImage.builder()
                .post(post)
                .sortIndex(FIRST_IMAGE_SORT_INDEX)
                .imgUrl(NULL_IMAGE_URL)
                .build();
    }

    /**
     * 너무 긴 URL의 PostImage를 생성합니다.
     */
    public static PostImage createTooLongUrlPostImage(Post post) {
        return PostImage.builder()
                .post(post)
                .sortIndex(FIRST_IMAGE_SORT_INDEX)
                .imgUrl(TOO_LONG_IMAGE_URL)
                .build();
    }

    /**
     * 음수 정렬 인덱스의 PostImage를 생성합니다.
     */
    public static PostImage createNegativeSortIndexPostImage(Post post) {
        return PostImage.builder()
                .post(post)
                .sortIndex(INVALID_SORT_INDEX_NEGATIVE)
                .imgUrl(VALID_IMAGE_URL_1)
                .build();
    }

    /**
     * 0 정렬 인덱스의 PostImage를 생성합니다.
     */
    public static PostImage createZeroSortIndexPostImage(Post post) {
        return PostImage.builder()
                .post(post)
                .sortIndex(INVALID_SORT_INDEX_ZERO)
                .imgUrl(VALID_IMAGE_URL_1)
                .build();
    }

    /**
     * 너무 큰 정렬 인덱스의 PostImage를 생성합니다.
     */
    public static PostImage createTooLargeSortIndexPostImage(Post post) {
        return PostImage.builder()
                .post(post)
                .sortIndex(INVALID_SORT_INDEX_TOO_LARGE)
                .imgUrl(VALID_IMAGE_URL_1)
                .build();
    }

    // ========== 정렬 테스트용 PostImage 생성 메서드 ==========


    // ========== 특수 상황 테스트용 메서드 ==========

    /**
     * 관리자 게시글의 PostImage를 생성합니다.
     */
    public static PostImage createAdminPostImage(Post post) {
        return PostImage.builder()
                .post(post)
                .sortIndex(FIRST_IMAGE_SORT_INDEX)
                .imgUrl(ADMIN_POST_IMAGE_URL)
                .build();
    }

    /**
     * 밴된 사용자 게시글의 PostImage를 생성합니다.
     */
    public static PostImage createBannedPostImage(Post post) {
        return PostImage.builder()
                .post(post)
                .sortIndex(FIRST_IMAGE_SORT_INDEX)
                .imgUrl(BANNED_POST_IMAGE_URL)
                .build();
    }

    /**
     * 삭제된 게시글의 PostImage를 생성합니다.
     */
    public static PostImage createDeletedPostImage(Post post) {
        return PostImage.builder()
                .post(post)
                .sortIndex(FIRST_IMAGE_SORT_INDEX)
                .imgUrl(DELETED_POST_IMAGE_URL)
                .build();
    }

    // ========== 업데이트 테스트용 메서드 ==========

    /**
     * 업데이트할 PostImage를 생성합니다.
     */
    public static PostImage createUpdatablePostImage(Post post) {
        return PostImage.builder()
                .post(post)
                .sortIndex(FIRST_IMAGE_SORT_INDEX)
                .imgUrl(VALID_IMAGE_URL_1)
                .build();
    }

    /**
     * 업데이트된 PostImage를 생성합니다.
     */
    public static PostImage createUpdatedPostImage(Post post) {
        return PostImage.builder()
                .post(post)
                .sortIndex(UPDATED_SORT_INDEX)
                .imgUrl(UPDATED_IMAGE_URL)
                .build();
    }

    // ========== 개수 제한 테스트용 메서드 ==========


    /**
     * 빈 PostImage 목록을 생성합니다.
     */
    public static List<PostImage> createEmptyPostImages() {
        return new ArrayList<>();
    }

    // ========== 중복 테스트용 메서드 ==========

    /**
     * 중복된 정렬 인덱스의 PostImage 목록을 생성합니다.
     */
    public static List<PostImage> createDuplicateSortIndexImages(Post post) {
        List<PostImage> images = new ArrayList<>();

        // 같은 정렬 인덱스로 여러 이미지 생성
        images.add(PostImage.builder()
                .post(post)
                .sortIndex(FIRST_IMAGE_SORT_INDEX)
                .imgUrl(VALID_IMAGE_URL_1)
                .build());

        images.add(PostImage.builder()
                .post(post)
                .sortIndex(FIRST_IMAGE_SORT_INDEX) // 중복된 인덱스
                .imgUrl(VALID_IMAGE_URL_2)
                .build());

        return images;
    }

    /**
     * 중복된 URL의 PostImage 목록을 생성합니다.
     */
    public static List<PostImage> createDuplicateUrlImages(Post post) {
        List<PostImage> images = new ArrayList<>();

        // 같은 URL로 여러 이미지 생성
        images.add(PostImage.builder()
                .post(post)
                .sortIndex(FIRST_IMAGE_SORT_INDEX)
                .imgUrl(VALID_IMAGE_URL_1)
                .build());

        images.add(PostImage.builder()
                .post(post)
                .sortIndex(SECOND_IMAGE_SORT_INDEX)
                .imgUrl(VALID_IMAGE_URL_1) // 중복된 URL
                .build());

        return images;
    }

    private PostImageFixture() {
        // 유틸리티 클래스 - 인스턴스화 방지
    }
}