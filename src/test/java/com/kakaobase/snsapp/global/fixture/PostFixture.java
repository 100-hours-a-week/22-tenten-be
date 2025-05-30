package com.kakaobase.snsapp.global.fixture;

import com.kakaobase.snsapp.domain.posts.entity.Post;

import java.util.ArrayList;
import java.util.List;

import static com.kakaobase.snsapp.global.constants.PostFixtureConstants.*;
import static com.kakaobase.snsapp.global.constants.MemberFixtureConstants.*;

public class PostFixture {

    // ========== 기본 게시글 생성 메서드 ==========

    /**
     * KBT 멤버의 기본 게시글을 생성합니다.
     */
    public static Post createKbtPost() {
        return Post.builder()
                .memberId(MEMBER_ID)
                .boardType(PANGYO_1_BOARD_TYPE)
                .content(POST_CONTENT)
                .youtubeUrl(POST_YOUTUBE_URL)
                .build();
    }

    /**
     * Non-KBT 멤버의 게시글을 생성합니다.
     */
    public static Post createNonKbtPost() {
        return Post.builder()
                .memberId(MEMBER_ID) // MemberFixture에서 Non-KBT 멤버 생성 시 사용할 ID
                .boardType(ALL_BOARD_TYPE)
                .content(NON_KBT_POST_CONTENT)
                .build();
    }

    /**
     * 관리자의 게시글을 생성합니다.
     */
    public static Post createAdminPost() {
        return Post.builder()
                .memberId(ADMIN_ID)
                .boardType(PANGYO_1_BOARD_TYPE)
                .content(ADMIN_POST_CONTENT)
                .build();
    }

    /**
     * 밴된 사용자의 게시글을 생성합니다.
     */
    public static Post createBannedUserPost() {
        return Post.builder()
                .memberId(BANNED_MEMBER_ID)
                .boardType(PANGYO_1_BOARD_TYPE)
                .content(BANNED_POST_CONTENT)
                .build();
    }

    // ========== 게시판 타입별 게시글 생성 메서드 ==========

    /**
     * 전체 게시판용 게시글을 생성합니다.
     */
    public static Post createAllBoardPost() {
        return Post.builder()
                .memberId(MEMBER_ID)
                .boardType(ALL_BOARD_TYPE)
                .content(VALID_CONTENT)
                .build();
    }

    /**
     * PANGYO_1 게시판용 게시글을 생성합니다.
     */
    public static Post createPangyo1Post() {
        return Post.builder()
                .memberId(MEMBER_ID)
                .boardType(PANGYO_1_BOARD_TYPE)
                .content(VALID_CONTENT)
                .build();
    }

    /**
     * PANGYO_2 게시판용 게시글을 생성합니다.
     */
    public static Post createPangyo2Post() {
        return Post.builder()
                .memberId(MEMBER_ID)
                .boardType(PANGYO_2_BOARD_TYPE)
                .content(VALID_CONTENT)
                .build();
    }

    /**
     * JEJU_1 게시판용 게시글을 생성합니다.
     */
    public static Post createJeju1Post() {
        return Post.builder()
                .memberId(MEMBER_ID)
                .boardType(JEJU_1_BOARD_TYPE)
                .content(VALID_CONTENT)
                .build();
    }

    /**
     * JEJU_2 게시판용 게시글을 생성합니다.
     */
    public static Post createJeju2Post() {
        return Post.builder()
                .memberId(MEMBER_ID)
                .boardType(JEJU_2_BOARD_TYPE)
                .content(VALID_CONTENT)
                .build();
    }

    /**
     * JEJU_3 게시판용 게시글을 생성합니다.
     */
    public static Post createJeju3Post() {
        return Post.builder()
                .memberId(MEMBER_ID)
                .boardType(JEJU_3_BOARD_TYPE)
                .content(VALID_CONTENT)
                .build();
    }

    // ========== 특별한 상태의 게시글 생성 메서드 ==========

    /**
     * 유튜브 URL이 포함된 게시글을 생성합니다.
     */
    public static Post createPostWithYoutube() {
        Post post = Post.builder()
                .memberId(MEMBER_ID)
                .boardType(PANGYO_1_BOARD_TYPE)
                .content(VALID_CONTENT)
                .youtubeUrl(VALID_YOUTUBE_URL)
                .build();

        post.setYoutubeSummary(POST_YOUTUBE_SUMMARY);
        return post;
    }

    /**
     * 내용만 있는 게시글을 생성합니다.
     */
    public static Post createContentOnlyPost() {
        return Post.builder()
                .memberId(MEMBER_ID)
                .boardType(PANGYO_1_BOARD_TYPE)
                .content(VALID_CONTENT)
                .build();
    }

    /**
     * 긴 내용의 게시글을 생성합니다.
     */
    public static Post createLongContentPost() {
        return Post.builder()
                .memberId(MEMBER_ID)
                .boardType(PANGYO_1_BOARD_TYPE)
                .content(LONG_VALID_CONTENT)
                .build();
    }

    /**
     * 유튜브 요약이 포함된 게시글을 생성합니다.
     */
    public static Post createPostWithYoutubeSummary() {
        Post post = Post.builder()
                .memberId(MEMBER_ID)
                .boardType(PANGYO_1_BOARD_TYPE)
                .content(VALID_CONTENT)
                .youtubeUrl(VALID_YOUTUBE_URL)
                .build();

        post.updateYoutubeSummary(POST_YOUTUBE_SUMMARY);
        return post;
    }

    // ========== 유효성 검증 실패용 게시글 생성 메서드 ==========

    /**
     * 빈 내용의 게시글을 생성합니다.
     */
    public static Post createEmptyContentPost() {
        return Post.builder()
                .memberId(MEMBER_ID)
                .boardType(PANGYO_1_BOARD_TYPE)
                .content(BLANK_CONTENT)
                .build();
    }

    /**
     * 공백만 있는 내용의 게시글을 생성합니다.
     */
    public static Post createWhitespaceContentPost() {
        return Post.builder()
                .memberId(MEMBER_ID)
                .boardType(PANGYO_1_BOARD_TYPE)
                .content(WHITESPACE_CONTENT)
                .build();
    }


    /**
     * 잘못된 유튜브 URL의 게시글을 생성합니다.
     */
    public static Post createInvalidYoutubePost() {
        return Post.builder()
                .memberId(MEMBER_ID)
                .boardType(PANGYO_1_BOARD_TYPE)
                .content(VALID_CONTENT)
                .youtubeUrl(INVALID_YOUTUBE_URL)
                .build();
    }

    /**
     * 형식이 잘못된 유튜브 URL의 게시글을 생성합니다.
     */
    public static Post createMalformedYoutubePost() {
        return Post.builder()
                .memberId(MEMBER_ID)
                .boardType(PANGYO_1_BOARD_TYPE)
                .content(VALID_CONTENT)
                .youtubeUrl(MALFORMED_YOUTUBE_URL)
                .build();
    }


    // ========== 다중 게시글 생성 메서드 ==========

    /**
     * 여러 개의 게시글 목록을 생성합니다.
     */
    public static List<Post> createMultiplePosts() {
        List<Post> posts = new ArrayList<>();

        for (int i = 0; i < MULTIPLE_POST_IDS.length; i++) {
            Post post = Post.builder()
                    .memberId(MEMBER_ID)
                    .boardType(PANGYO_1_BOARD_TYPE)
                    .content(MULTIPLE_POST_CONTENTS[i])
                    .build();
            posts.add(post);
        }

        return posts;
    }

    /**
     * 서로 다른 게시판의 게시글 목록을 생성합니다.
     */
    public static List<Post> createDifferentBoardPosts() {
        List<Post> posts = new ArrayList<>();

        posts.add(createAllBoardPost());
        posts.add(createPangyo1Post());
        posts.add(createPangyo2Post());
        posts.add(createJeju1Post());
        posts.add(createJeju2Post());
        posts.add(createJeju3Post());

        return posts;
    }




    // ========== 권한 테스트용 메서드 ==========

    /**
     * 권한이 없는 사용자가 접근하려는 게시글을 생성합니다.
     */
    public static Post createUnauthorizedAccessPost() {
        return Post.builder()
                .memberId(UNAUTHORIZED_MEMBER_ID)
                .boardType(PANGYO_1_BOARD_TYPE)
                .content(VALID_CONTENT)
                .build();
    }

    /**
     * 다른 사용자의 게시글을 생성합니다 (삭제 권한 테스트용).
     */
    public static Post createOtherUserPost() {
        return Post.builder()
                .memberId(ADMIN_ID) // 다른 사용자 ID
                .boardType(PANGYO_1_BOARD_TYPE)
                .content(VALID_CONTENT)
                .build();
    }

    // ========== 조합 테스트용 메서드 ==========

    /**
     * 모든 요소가 포함된 완전한 게시글을 생성합니다.
     */
    public static Post createCompletePost() {
        Post post = Post.builder()
                .memberId(MEMBER_ID)
                .boardType(PANGYO_1_BOARD_TYPE)
                .content(VALID_CONTENT)
                .youtubeUrl(VALID_YOUTUBE_URL)
                .build();

        post.updateYoutubeSummary(POST_YOUTUBE_SUMMARY);
        post.increaseLikeCount();
        post.increaseCommentCount();

        return post;
    }


    /**
     * 내용, 유튜브 URL, 이미지 URL이 모두 비어있는 게시글을 생성합니다.
     * (유효성 검증 실패 테스트용)
     */
    public static Post createAllEmptyPost() {
        return Post.builder()
                .memberId(MEMBER_ID)
                .boardType(PANGYO_1_BOARD_TYPE)
                .content(BLANK_CONTENT)
                .youtubeUrl(BLANK_YOUTUBE_URL)
                .build();
    }



    private PostFixture() {
        // 유틸리티 클래스 - 인스턴스화 방지
    }
}