package com.kakaobase.snsapp.global.constants;

import com.kakaobase.snsapp.domain.posts.entity.Post;

public class PostFixtureConstants {

    // ========== 기본 Mock 데이터 ==========

    public static final Long POST_ID = 1L;
    public static final Post.BoardType POST_BOARD_TYPE = Post.BoardType.PANGYO_1;
    public static final String POST_CONTENT = "테스트 게시글 내용입니다.";
    public static final String POST_YOUTUBE_URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
    public static final String POST_YOUTUBE_SUMMARY = "테스트 유튜브 영상 요약";
    public static final Integer POST_LIKE_COUNT = 0;
    public static final Integer POST_COMMENT_COUNT = 0;

    // ========== 다양한 게시판 타입 데이터 ==========

    public static final Post.BoardType ALL_BOARD_TYPE = Post.BoardType.ALL;
    public static final Post.BoardType PANGYO_1_BOARD_TYPE = Post.BoardType.PANGYO_1;
    public static final Post.BoardType PANGYO_2_BOARD_TYPE = Post.BoardType.PANGYO_2;
    public static final Post.BoardType JEJU_1_BOARD_TYPE = Post.BoardType.JEJU_1;
    public static final Post.BoardType JEJU_2_BOARD_TYPE = Post.BoardType.JEJU_2;
    public static final Post.BoardType JEJU_3_BOARD_TYPE = Post.BoardType.JEJU_3;

    // ========== 관리자용 게시글 데이터 ==========

    public static final Long ADMIN_POST_ID = 2L;
    public static final String ADMIN_POST_CONTENT = "관리자 게시글 내용";

    // ========== 밴된 사용자 게시글 데이터 ==========

    public static final Long BANNED_POST_ID = 3L;
    public static final String BANNED_POST_CONTENT = "밴된 사용자 게시글";

    // ========== 삭제된 게시글 데이터 ==========

    public static final Long DELETED_POST_ID = 4L;
    public static final String DELETED_POST_CONTENT = "삭제된 게시글 내용";

    // ========== 좋아요 관련 데이터 ==========

    public static final Long LIKED_POST_ID = 5L;
    public static final Integer LIKED_POST_LIKE_COUNT = 5;

    // ========== Non-KBT 사용자 게시글 데이터 ==========

    public static final Long NON_KBT_POST_ID = 6L;
    public static final String NON_KBT_POST_CONTENT = "Non-KBT 사용자 게시글";

    // ========== 페이징 관련 상수 ==========

    public static final Integer DEFAULT_LIMIT = 12;
    public static final Integer CUSTOM_LIMIT = 20;
    public static final Long CURSOR_POST_ID = 10L;

    // ========== 테스트용 유효한 데이터 상수 ==========

    // 유효한 게시글 내용들
    public static final String VALID_CONTENT = "유효한 게시글 내용입니다.";
    public static final String VALID_CONTENT_ALT = "또 다른 유효한 게시글 내용";
    public static final String LONG_VALID_CONTENT = "매우 긴 게시글 내용입니다. ".repeat(50); // 3000자 이내
    public static final String VALID_YOUTUBE_URL = "https://www.youtube.com/watch?v=abcd1234567";




    // ========== 유효성 검증 실패 케이스 상수 ==========

    // 빈 콘텐츠 관련
    public static final String BLANK_CONTENT = "";
    public static final String NULL_CONTENT = null;
    public static final String WHITESPACE_CONTENT = "   ";

    // 콘텐츠 길이 초과
    public static final String TOO_LONG_CONTENT = "매우 긴 콘텐츠입니다. ".repeat(200); // 3000자 초과

    // 잘못된 유튜브 URL
    public static final String INVALID_YOUTUBE_URL = "https://www.google.com/watch?v=invalid";
    public static final String MALFORMED_YOUTUBE_URL = "not-a-valid-url";
    public static final String BLANK_YOUTUBE_URL = "";


    // ========== 권한 검증 관련 상수 ==========

    // 존재하지 않는 ID들
    public static final Long NON_EXISTENT_POST_ID = 999L;

    // 권한이 없는 게시판 접근 시나리오
    public static final Long UNAUTHORIZED_MEMBER_ID = 100L;

    // ========== 게시글 수정 관련 상수 ==========

    public static final String UPDATED_POST_CONTENT = "수정된 게시글 내용";
    public static final String UPDATED_YOUTUBE_URL = "https://www.youtube.com/watch?v=updated123";
    public static final String UPDATED_YOUTUBE_SUMMARY = "수정된 유튜브 요약";

    // ========== 카운트 관련 상수 ==========

    public static final Integer INITIAL_LIKE_COUNT = 0;
    public static final Integer INCREMENTED_LIKE_COUNT = 1;
    public static final Integer DECREMENTED_LIKE_COUNT = 0;
    public static final Integer MULTIPLE_LIKES_COUNT = 10;

    public static final Integer INITIAL_COMMENT_COUNT = 0;
    public static final Integer INCREMENTED_COMMENT_COUNT = 1;
    public static final Integer DECREMENTED_COMMENT_COUNT = 0;
    public static final Integer MULTIPLE_COMMENTS_COUNT = 5;

    // ========== API 응답 관련 상수 ==========

    public static final String SUCCESS_MESSAGE = "성공적으로 처리되었습니다.";
    public static final String CREATE_SUCCESS_MESSAGE = "게시글이 성공적으로 생성되었습니다.";
    public static final String DELETE_SUCCESS_MESSAGE = "게시글이 성공적으로 삭제되었습니다.";
    public static final String LIKE_SUCCESS_MESSAGE = "좋아요가 처리되었습니다.";
    public static final String UNLIKE_SUCCESS_MESSAGE = "좋아요가 취소되었습니다.";

    // ========== 예외 메시지 관련 상수 ==========

    public static final String POST_NOT_FOUND_MESSAGE = "게시글을 찾을 수 없습니다.";
    public static final String DELETED_POST_ACCESS_MESSAGE = "삭제된 게시글에 접근할 수 없습니다.";
    public static final String UNAUTHORIZED_ACCESS_MESSAGE = "접근 권한이 없습니다.";
    public static final String BANNED_USER_ACCESS_MESSAGE = "밴된 사용자는 게시글을 작성할 수 없습니다.";
    public static final String EMPTY_CONTENT_MESSAGE = "게시글 내용이 비어있습니다.";
    public static final String UNAUTHORIZED_DELETE_MESSAGE = "게시글 삭제 권한이 없습니다.";

    // ========== 다중 게시글 테스트용 데이터 ==========

    public static final Long[] MULTIPLE_POST_IDS = {1L, 2L, 3L, 4L, 5L};
    public static final String[] MULTIPLE_POST_CONTENTS = {
            "첫 번째 게시글",
            "두 번째 게시글",
            "세 번째 게시글",
            "네 번째 게시글",
            "다섯 번째 게시글"
    };

    // ========== PostLike 관련 상수 ==========

    public static final Long POST_LIKE_POST_ID = 1L;
    public static final Long ALREADY_LIKED_POST_ID = 2L;

    private PostFixtureConstants() {
        // 유틸리티 클래스 - 인스턴스화 방지
    }
}