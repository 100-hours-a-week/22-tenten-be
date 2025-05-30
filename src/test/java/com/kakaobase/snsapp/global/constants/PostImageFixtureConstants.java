package com.kakaobase.snsapp.global.constants;

public class PostImageFixtureConstants {

    // ========== 기본 PostImage 데이터 ==========

    public static final Long POST_IMAGE_ID = 1L;
    public static final Long POST_IMAGE_POST_ID = 1L;
    public static final Integer POST_IMAGE_SORT_INDEX = 1;
    public static final String POST_IMAGE_URL = "https://example.com/images/post-image-1.jpg";

    // ========== 다양한 이미지 URL 데이터 ==========

    public static final String VALID_IMAGE_URL_1 = "https://example.com/image1.jpg";
    public static final String VALID_IMAGE_URL_2 = "https://example.com/image2.png";
    public static final String VALID_IMAGE_URL_3 = "https://example.com/image3.gif";
    public static final String VALID_IMAGE_URL_4 = "https://example.com/image4.webp";
    public static final String VALID_IMAGE_URL_5 = "https://example.com/image5.jpeg";

    // ========== 다중 이미지 정렬 인덱스 ==========

    public static final Integer FIRST_IMAGE_SORT_INDEX = 1;
    public static final Integer SECOND_IMAGE_SORT_INDEX = 2;
    public static final Integer THIRD_IMAGE_SORT_INDEX = 3;
    public static final Integer FOURTH_IMAGE_SORT_INDEX = 4;
    public static final Integer FIFTH_IMAGE_SORT_INDEX = 5;

    // ========== 이미지 개수 제한 관련 상수 ==========

    public static final Integer MAX_IMAGE_COUNT = 5;
    public static final Integer MIN_IMAGE_COUNT = 0;
    public static final Integer TYPICAL_IMAGE_COUNT = 3;

    // ========== 다양한 게시글의 이미지 데이터 ==========

    public static final Long ADMIN_POST_IMAGE_POST_ID = 2L;
    public static final String ADMIN_POST_IMAGE_URL = "https://example.com/admin/image.jpg";

    public static final Long BANNED_POST_IMAGE_POST_ID = 3L;
    public static final String BANNED_POST_IMAGE_URL = "https://example.com/banned/image.jpg";

    public static final Long DELETED_POST_IMAGE_POST_ID = 4L;
    public static final String DELETED_POST_IMAGE_URL = "https://example.com/deleted/image.jpg";

    // ========== 이미지 업데이트 테스트 데이터 ==========

    public static final String UPDATED_IMAGE_URL = "https://example.com/updated/image.jpg";
    public static final Integer UPDATED_SORT_INDEX = 2;

    // ========== 유효성 검증 실패 케이스 상수 ==========

    // 잘못된 이미지 URL
    public static final String INVALID_IMAGE_URL = "not-a-valid-image-url";
    public static final String MALFORMED_IMAGE_URL = "http://invalid-url";
    public static final String BLANK_IMAGE_URL = "";
    public static final String NULL_IMAGE_URL = null;
    public static final String TOO_LONG_IMAGE_URL = "https://example.com/" + "a".repeat(500) + ".jpg"; // 512자 초과

    // 잘못된 정렬 인덱스
    public static final Integer INVALID_SORT_INDEX_NEGATIVE = -1;
    public static final Integer INVALID_SORT_INDEX_ZERO = 0;
    public static final Integer INVALID_SORT_INDEX_TOO_LARGE = 100;

    // ========== 존재하지 않는 데이터 ==========

    public static final Long NON_EXISTENT_IMAGE_ID = 999L;
    public static final Long NON_EXISTENT_IMAGE_POST_ID = 999L;

    // ========== 이미지 업로드 관련 상수 ==========

    public static final String IMAGE_UPLOAD_SUCCESS_MESSAGE = "이미지가 성공적으로 업로드되었습니다.";
    public static final String IMAGE_DELETE_SUCCESS_MESSAGE = "이미지가 성공적으로 삭제되었습니다.";
    public static final String IMAGE_UPDATE_SUCCESS_MESSAGE = "이미지가 성공적으로 수정되었습니다.";

    // ========== 이미지 예외 메시지 ==========

    public static final String IMAGE_NOT_FOUND_MESSAGE = "이미지를 찾을 수 없습니다.";
    public static final String POST_NOT_FOUND_FOR_IMAGE_MESSAGE = "이미지를 추가할 게시글을 찾을 수 없습니다.";
    public static final String INVALID_IMAGE_URL_MESSAGE = "유효하지 않은 이미지 URL입니다.";
    public static final String INVALID_SORT_INDEX_MESSAGE = "유효하지 않은 정렬 순서입니다.";
    public static final String MAX_IMAGE_COUNT_EXCEEDED_MESSAGE = "최대 이미지 개수를 초과했습니다.";
    public static final String IMAGE_URL_TOO_LONG_MESSAGE = "이미지 URL이 너무 깁니다.";

    // ========== 이미지 파일 확장자 관련 상수 ==========

    public static final String JPG_EXTENSION = ".jpg";
    public static final String JPEG_EXTENSION = ".jpeg";
    public static final String PNG_EXTENSION = ".png";
    public static final String GIF_EXTENSION = ".gif";
    public static final String WEBP_EXTENSION = ".webp";

    public static final String[] VALID_IMAGE_EXTENSIONS = {
            JPG_EXTENSION, JPEG_EXTENSION, PNG_EXTENSION, GIF_EXTENSION, WEBP_EXTENSION
    };

    // ========== 이미지 크기 관련 상수 ==========

    public static final Long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024; // 10MB
    public static final Long TYPICAL_IMAGE_SIZE_BYTES = 2L * 1024 * 1024; // 2MB
    public static final Integer MAX_IMAGE_WIDTH = 4096;
    public static final Integer MAX_IMAGE_HEIGHT = 4096;
    public static final Integer TYPICAL_IMAGE_WIDTH = 1920;
    public static final Integer TYPICAL_IMAGE_HEIGHT = 1080;


    private PostImageFixtureConstants() {
        // 유틸리티 클래스 - 인스턴스화 방지
    }
}