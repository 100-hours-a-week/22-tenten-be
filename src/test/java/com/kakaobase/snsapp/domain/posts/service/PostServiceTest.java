package com.kakaobase.snsapp.domain.posts.service;

import com.kakaobase.snsapp.domain.follow.repository.FollowRepository;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.service.MemberService;
import com.kakaobase.snsapp.domain.posts.dto.PostRequestDto;
import com.kakaobase.snsapp.domain.posts.dto.PostResponseDto;
import com.kakaobase.snsapp.domain.posts.entity.Post;
import com.kakaobase.snsapp.domain.posts.entity.PostImage;
import com.kakaobase.snsapp.domain.posts.exception.PostErrorCode;
import com.kakaobase.snsapp.domain.posts.exception.PostException;
import com.kakaobase.snsapp.domain.posts.repository.PostImageRepository;
import com.kakaobase.snsapp.domain.posts.repository.PostRepository;
import com.kakaobase.snsapp.global.common.s3.service.S3Service;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import com.kakaobase.snsapp.global.fixture.post.PostFixture;
import com.kakaobase.snsapp.global.fixture.post.PostImageFixture;
import com.kakaobase.snsapp.global.fixture.post.PostRequestDtoFixture;
import com.kakaobase.snsapp.global.fixture.member.MemberFixture;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.kakaobase.snsapp.global.constants.MemberFixtureConstants.*;
import static com.kakaobase.snsapp.global.constants.PostFixtureConstants.*;
import static com.kakaobase.snsapp.global.constants.PostImageFixtureConstants.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostService 단위 테스트")
class PostServiceTest {

    @InjectMocks
    private PostService postService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private S3Service s3Service;

    @Mock
    private MemberService memberService;

    @Mock
    private YouTubeSummaryService youtubeSummaryService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private PostLikeService postLikeService;

    @Mock
    private FollowRepository followRepository;

    @Mock
    private EntityManager entityManager;

    private Post mockPost;
    private Post mockAdminPost;
    private Member mockMember;
    private Member mockFollowing;
    private PostRequestDto.PostCreateRequestDto contentOnlyRequest;
    private PostRequestDto.PostCreateRequestDto imageRequest;
    private PostRequestDto.PostCreateRequestDto youtubeRequest;
    private PostRequestDto.PostCreateRequestDto completeRequest;

    @BeforeEach
    void setUp() {
        // Mock 게시글 생성
        mockPost = PostFixture.createKbtPost();
        ReflectionTestUtils.setField(mockPost, "id", POST_ID);

        mockAdminPost = PostFixture.createAdminPost();
        ReflectionTestUtils.setField(mockAdminPost, "id", ADMIN_POST_ID);

        // Mock Members 생성 (Follow 관계용)
        mockMember = MemberFixture.createKbtMember();
        ReflectionTestUtils.setField(mockMember, "id", MEMBER_ID);

        mockFollowing = MemberFixture.createNonKbtMember();
        ReflectionTestUtils.setField(mockFollowing, "id", ADMIN_ID);

        // Mock 요청 DTO 생성
        contentOnlyRequest = PostRequestDtoFixture.createContentOnlyRequest();
        imageRequest = PostRequestDtoFixture.createImageOnlyRequest();
        youtubeRequest = PostRequestDtoFixture.createYoutubeOnlyRequest();
        completeRequest = PostRequestDtoFixture.createBasicPostCreateRequest();
    }

    // ========== createPost() 메서드 테스트 ==========

    @Test
    @DisplayName("내용만 있는 게시글 생성 - 정상적으로 게시글이 생성되는지 확인")
    void createPost_ContentOnly_Success() {
        // given
        given(postRepository.save(any(Post.class))).willReturn(mockPost);

        // when
        Post result = postService.createPost(PANGYO_1_BOARD_TYPE, contentOnlyRequest, MEMBER_ID);

        // then
        assertThat(result)
                .isNotNull()
                .satisfies(post -> {
                    assertThat(post.getMemberId()).isEqualTo(MEMBER_ID);
                    assertThat(post.getBoardType()).isEqualTo(PANGYO_1_BOARD_TYPE);
                    assertThat(post.getContent()).isEqualTo(POST_CONTENT);
                });

        verify(postRepository).save(any(Post.class));
        verify(postImageRepository, never()).save(any(PostImage.class));
        verify(applicationEventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("이미지가 포함된 게시글 생성 - 이미지와 함께 게시글이 생성되는지 확인")
    void createPost_WithImage_Success() {
        // given
        given(s3Service.isValidImageUrl(VALID_IMAGE_URL_1)).willReturn(true);
        given(postRepository.save(any(Post.class))).willReturn(mockPost);

        // when
        Post result = postService.createPost(PANGYO_1_BOARD_TYPE, imageRequest, MEMBER_ID);

        // then
        assertThat(result).isNotNull();

        verify(s3Service).isValidImageUrl(VALID_IMAGE_URL_1);
        verify(postRepository).save(any(Post.class));
        verify(postImageRepository).save(any(PostImage.class));
        verify(applicationEventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("유튜브 URL이 포함된 게시글 생성 - 유튜브 요약 처리가 예약되는지 확인")
    void createPost_WithYoutube_Success() {
        // given
        given(postRepository.save(any(Post.class))).willReturn(mockPost);

        try (MockedStatic<TransactionSynchronizationManager> mockedTxManager =
                     mockStatic(TransactionSynchronizationManager.class)) {

            // when
            Post result = postService.createPost(PANGYO_1_BOARD_TYPE, youtubeRequest, MEMBER_ID);

            // then
            assertThat(result).isNotNull();

            verify(postRepository).save(any(Post.class));
            verify(applicationEventPublisher).publishEvent(any());

            mockedTxManager.verify(() ->
                    TransactionSynchronizationManager.registerSynchronization(any()));
        }
    }

    @Test
    @DisplayName("모든 요소가 포함된 게시글 생성 - 완전한 게시글이 생성되는지 확인")
    void createPost_WithAllElements_Success() {
        // given
        given(s3Service.isValidImageUrl(VALID_IMAGE_URL_1)).willReturn(true);
        given(postRepository.save(any(Post.class))).willReturn(mockPost);

        try (MockedStatic<TransactionSynchronizationManager> mockedTxManager =
                     mockStatic(TransactionSynchronizationManager.class)) {

            // when
            Post result = postService.createPost(PANGYO_1_BOARD_TYPE, completeRequest, MEMBER_ID);

            // then
            assertThat(result).isNotNull();

            verify(s3Service).isValidImageUrl(VALID_IMAGE_URL_1);
            verify(postRepository).save(any(Post.class));
            verify(postImageRepository).save(any(PostImage.class));
            verify(applicationEventPublisher).publishEvent(any());

            mockedTxManager.verify(() ->
                    TransactionSynchronizationManager.registerSynchronization(any()));
        }
    }

    @Test
    @DisplayName("잘못된 이미지 URL로 게시글 생성 - PostException이 발생하는지 확인")
    void createPost_InvalidImageUrl_ThrowsException() {
        // given
        PostRequestDto.PostCreateRequestDto invalidImageRequest =
                PostRequestDtoFixture.createImageOnlyRequest();

        given(s3Service.isValidImageUrl(VALID_IMAGE_URL_1)).willReturn(false);

        // when & then
        assertThatThrownBy(() ->
                postService.createPost(PANGYO_1_BOARD_TYPE, invalidImageRequest, MEMBER_ID))
                .isInstanceOf(PostException.class)
                .satisfies(exception -> {
                    PostException postException = (PostException) exception;
                    assertThat(postException.getErrorCode()).isEqualTo(PostErrorCode.INVALID_IMAGE_URL);
                });

        verify(s3Service).isValidImageUrl(VALID_IMAGE_URL_1);
        verify(postRepository, never()).save(any());
    }

    @Test
    @DisplayName("모든 내용이 비어있는 게시글 생성 - 유효성 검증은 DTO에서 처리하므로 정상 처리")
    void createPost_EmptyContent_Success() {
        // given
        PostRequestDto.PostCreateRequestDto emptyRequest =
                PostRequestDtoFixture.createEmptyRequest();

        given(postRepository.save(any(Post.class))).willReturn(mockPost);

        // when
        Post result = postService.createPost(PANGYO_1_BOARD_TYPE, emptyRequest, MEMBER_ID);

        // then
        assertThat(result).isNotNull();
        verify(postRepository).save(any(Post.class));
    }

    // ========== getPostDetail() 메서드 테스트 ==========

    @Test
    @DisplayName("본인 게시글 조회 - 본인 게시글 여부가 true로 설정되는지 확인")
    void getPostDetail_OwnPost_Success() {
        // given
        Long postId = POST_ID;
        Map<String, String> memberInfo = Map.of(
                "nickname", MEMBER_NICKNAME,
                "imageUrl", MEMBER_PROFILE_IMG_URL
        );
        List<PostImage> postImages = List.of(PostImageFixture.createBasicPostImage(mockPost));

        given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));
        given(memberService.getMemberInfo(MEMBER_ID)).willReturn(memberInfo);
        given(postLikeService.isLikedByMember(postId, MEMBER_ID)).willReturn(false);
        given(postImageRepository.findByPostIdOrderBySortIndexAsc(postId)).willReturn(postImages);
        given(entityManager.getReference(Member.class, MEMBER_ID)).willReturn(mockMember);
        given(entityManager.getReference(Member.class, mockPost.getMemberId())).willReturn(mockMember);
        given(followRepository.existsByFollowerUserAndFollowingUser(mockMember, mockMember)).willReturn(false);

        // when
        PostResponseDto.PostDetailResponse result = postService.getPostDetail(postId, MEMBER_ID);

        // then
        assertThat(result).isNotNull();
        assertThat(result.data().isMine()).isTrue();
        assertThat(result.data().user().nickname()).isEqualTo(MEMBER_NICKNAME);

        verify(postRepository).findById(postId);
        verify(memberService).getMemberInfo(MEMBER_ID);
        verify(postLikeService).isLikedByMember(postId, MEMBER_ID);
        verify(followRepository).existsByFollowerUserAndFollowingUser(any(Member.class), any(Member.class));
    }

    @Test
    @DisplayName("다른 사용자 게시글 조회 시 팔로우 상태 확인 - 팔로우 여부가 정확히 반환되는지 확인")
    void getPostDetail_OtherPostWithFollowStatus_Success() {
        // given
        Long postId = ADMIN_POST_ID;
        Map<String, String> memberInfo = Map.of(
                "nickname", ADMIN_NICKNAME,
                "imageUrl", MEMBER_PROFILE_IMG_URL
        );

        given(postRepository.findById(postId)).willReturn(Optional.of(mockAdminPost));
        given(memberService.getMemberInfo(ADMIN_ID)).willReturn(memberInfo);
        given(postLikeService.isLikedByMember(postId, MEMBER_ID)).willReturn(false);
        given(postImageRepository.findByPostIdOrderBySortIndexAsc(postId)).willReturn(List.of());
        given(entityManager.getReference(Member.class, MEMBER_ID)).willReturn(mockMember);
        given(entityManager.getReference(Member.class, ADMIN_ID)).willReturn(mockFollowing);
        given(followRepository.existsByFollowerUserAndFollowingUser(mockMember, mockFollowing)).willReturn(true);

        // when
        PostResponseDto.PostDetailResponse result = postService.getPostDetail(postId, MEMBER_ID);

        // then
        assertThat(result).isNotNull();
        assertThat(result.data().isMine()).isFalse();
        assertThat(result.data().user().nickname()).isEqualTo(ADMIN_NICKNAME);

        verify(postRepository).findById(postId);
        verify(memberService).getMemberInfo(ADMIN_ID);
        verify(followRepository).existsByFollowerUserAndFollowingUser(mockMember, mockFollowing);
    }

    @Test
    @DisplayName("팔로우하지 않은 사용자의 게시글 조회 - isFollowing이 false로 설정되는지 확인")
    void getPostDetail_NotFollowingUser_Success() {
        // given
        Long postId = ADMIN_POST_ID;
        Map<String, String> memberInfo = Map.of(
                "nickname", ADMIN_NICKNAME,
                "imageUrl", MEMBER_PROFILE_IMG_URL
        );

        given(postRepository.findById(postId)).willReturn(Optional.of(mockAdminPost));
        given(memberService.getMemberInfo(ADMIN_ID)).willReturn(memberInfo);
        given(postLikeService.isLikedByMember(postId, MEMBER_ID)).willReturn(false);
        given(postImageRepository.findByPostIdOrderBySortIndexAsc(postId)).willReturn(List.of());
        given(entityManager.getReference(Member.class, MEMBER_ID)).willReturn(mockMember);
        given(entityManager.getReference(Member.class, ADMIN_ID)).willReturn(mockFollowing);
        given(followRepository.existsByFollowerUserAndFollowingUser(mockMember, mockFollowing)).willReturn(false);

        // when
        PostResponseDto.PostDetailResponse result = postService.getPostDetail(postId, MEMBER_ID);

        // then
        assertThat(result).isNotNull();
        assertThat(result.data().isMine()).isFalse();

        verify(followRepository).existsByFollowerUserAndFollowingUser(mockMember, mockFollowing);
    }

    @Test
    @DisplayName("로그인하지 않은 상태로 조회 - 로그인 관련 정보가 false로 설정되는지 확인")
    void getPostDetail_NotLoggedIn_Success() {
        // given
        Long postId = POST_ID;
        Map<String, String> memberInfo = Map.of(
                "nickname", MEMBER_NICKNAME,
                "imageUrl", MEMBER_PROFILE_IMG_URL
        );

        given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));
        given(memberService.getMemberInfo(MEMBER_ID)).willReturn(memberInfo);
        given(postImageRepository.findByPostIdOrderBySortIndexAsc(postId)).willReturn(List.of());
        given(entityManager.getReference(Member.class, null)).willReturn(null);

        // when
        PostResponseDto.PostDetailResponse result = postService.getPostDetail(postId, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.data().isMine()).isFalse();
        assertThat(result.data().isLiked()).isFalse();

        verify(postRepository).findById(postId);
        verify(memberService).getMemberInfo(MEMBER_ID);
        verify(postLikeService, never()).isLikedByMember(any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 게시글 조회 - PostException이 발생하는지 확인")
    void getPostDetail_PostNotFound_ThrowsException() {
        // given
        Long nonExistentPostId = NON_EXISTENT_POST_ID;
        given(postRepository.findById(nonExistentPostId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                postService.getPostDetail(nonExistentPostId, MEMBER_ID))
                .isInstanceOf(PostException.class)
                .satisfies(exception -> {
                    PostException postException = (PostException) exception;
                    assertThat(postException.getErrorCode()).isEqualTo(GeneralErrorCode.RESOURCE_NOT_FOUND);
                });

        verify(postRepository).findById(nonExistentPostId);
    }

    // ========== getPostList() 메서드 테스트 ==========

    @Test
    @DisplayName("기본 게시글 목록 조회 - 정상적으로 목록이 반환되는지 확인")
    void getPostList_BasicRequest_Success() {
        // given
        String postType = "PANGYO_1";
        List<Post> mockPosts = List.of(mockPost);
        Map<Long, Map<String, String>> memberInfoMap = Map.of(
                MEMBER_ID, Map.of("nickname", MEMBER_NICKNAME, "imageUrl", MEMBER_PROFILE_IMG_URL)
        );

        given(postRepository.findTopNByBoardTypeOrderByCreatedAtDescIdDesc(PANGYO_1_BOARD_TYPE, DEFAULT_LIMIT))
                .willReturn(mockPosts);
        given(memberService.getMemberInfoMapByIds(any())).willReturn(memberInfoMap);
        given(postImageRepository.findFirstImagesByPostIds(any())).willReturn(List.of());

        // when
        PostResponseDto.PostListResponse result =
                postService.getPostList(postType, DEFAULT_LIMIT, null, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.data()).hasSize(1);
        assertThat(result.message()).isEqualTo("게시글을 불러오는데 성공하였습니다");

        verify(postRepository).findTopNByBoardTypeOrderByCreatedAtDescIdDesc(PANGYO_1_BOARD_TYPE, DEFAULT_LIMIT);
        verify(postLikeService, never()).findLikedPostIdsByMember(any(), any());
    }

    @Test
    @DisplayName("로그인한 사용자의 게시글 목록 조회 - 팔로우 정보가 포함되는지 확인")
    void getPostList_LoggedInUserWithFollowInfo_Success() {
        // given
        String postType = "PANGYO_1";
        List<Post> mockPosts = List.of(mockPost);
        Map<Long, Map<String, String>> memberInfoMap = Map.of(
                MEMBER_ID, Map.of("nickname", MEMBER_NICKNAME, "imageUrl", MEMBER_PROFILE_IMG_URL)
        );
        List<Long> likedPostIds = List.of();

        given(postRepository.findTopNByBoardTypeOrderByCreatedAtDescIdDesc(PANGYO_1_BOARD_TYPE, DEFAULT_LIMIT))
                .willReturn(mockPosts);
        given(memberService.getMemberInfoMapByIds(any())).willReturn(memberInfoMap);
        given(postImageRepository.findFirstImagesByPostIds(any())).willReturn(List.of());
        given(postLikeService.findLikedPostIdsByMember(ADMIN_ID, mockPosts)).willReturn(likedPostIds);
        given(entityManager.getReference(Member.class, ADMIN_ID)).willReturn(mockMember);
        given(entityManager.getReference(Member.class, MEMBER_ID)).willReturn(mockFollowing);
        given(followRepository.existsByFollowerUserAndFollowingUser(mockMember, mockFollowing)).willReturn(true);

        // when
        PostResponseDto.PostListResponse result =
                postService.getPostList(postType, DEFAULT_LIMIT, null, ADMIN_ID);

        // then
        assertThat(result).isNotNull();
        assertThat(result.data()).hasSize(1);

        verify(postRepository).findTopNByBoardTypeOrderByCreatedAtDescIdDesc(PANGYO_1_BOARD_TYPE, DEFAULT_LIMIT);
        verify(postLikeService).findLikedPostIdsByMember(ADMIN_ID, mockPosts);
        verify(followRepository).existsByFollowerUserAndFollowingUser(any(Member.class), any(Member.class));
    }

    @Test
    @DisplayName("커서 기반 페이징 - 커서 이후 게시글들이 조회되는지 확인")
    void getPostList_WithCursor_Success() {
        // given
        String postType = "PANGYO_1";
        List<Post> mockPosts = List.of(mockPost);
        Map<Long, Map<String, String>> memberInfoMap = Map.of(
                MEMBER_ID, Map.of("nickname", MEMBER_NICKNAME, "imageUrl", MEMBER_PROFILE_IMG_URL)
        );

        given(postRepository.findByBoardTypeAndIdLessThanOrderByIdDesc(PANGYO_1_BOARD_TYPE, CURSOR_POST_ID, DEFAULT_LIMIT))
                .willReturn(mockPosts);
        given(memberService.getMemberInfoMapByIds(any())).willReturn(memberInfoMap);
        given(postImageRepository.findFirstImagesByPostIds(any())).willReturn(List.of());

        // when
        PostResponseDto.PostListResponse result =
                postService.getPostList(postType, DEFAULT_LIMIT, CURSOR_POST_ID, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.data()).hasSize(1);

        verify(postRepository).findByBoardTypeAndIdLessThanOrderByIdDesc(PANGYO_1_BOARD_TYPE, CURSOR_POST_ID, DEFAULT_LIMIT);
        verify(postLikeService, never()).findLikedPostIdsByMember(any(), any());
    }

    @Test
    @DisplayName("로그인한 사용자의 좋아요 정보 포함 - 좋아요 정보가 정확히 포함되는지 확인")
    void getPostList_WithLikedInfo_Success() {
        // given
        String postType = "PANGYO_1";
        List<Post> mockPosts = List.of(mockPost);
        Map<Long, Map<String, String>> memberInfoMap = Map.of(
                MEMBER_ID, Map.of("nickname", MEMBER_NICKNAME, "imageUrl", MEMBER_PROFILE_IMG_URL)
        );
        List<Long> likedPostIds = List.of(POST_ID);

        given(postRepository.findTopNByBoardTypeOrderByCreatedAtDescIdDesc(PANGYO_1_BOARD_TYPE, DEFAULT_LIMIT))
                .willReturn(mockPosts);
        given(memberService.getMemberInfoMapByIds(any())).willReturn(memberInfoMap);
        given(postImageRepository.findFirstImagesByPostIds(any())).willReturn(List.of());
        given(postLikeService.findLikedPostIdsByMember(MEMBER_ID, mockPosts)).willReturn(likedPostIds);

        // 자신의 게시글이므로 follower와 following이 같은 객체여야 함
        given(entityManager.getReference(Member.class, MEMBER_ID)).willReturn(mockMember);
        given(followRepository.existsByFollowerUserAndFollowingUser(mockMember, mockMember)).willReturn(false);

        // when
        PostResponseDto.PostListResponse result =
                postService.getPostList(postType, DEFAULT_LIMIT, null, MEMBER_ID);

        // then
        assertThat(result).isNotNull();
        assertThat(result.data().get(0).isLiked()).isTrue();

        verify(postLikeService).findLikedPostIdsByMember(MEMBER_ID, mockPosts);
        verify(followRepository).existsByFollowerUserAndFollowingUser(mockMember, mockMember);
    }

    @Test
    @DisplayName("다른 사용자 게시글 목록 조회 시 팔로우 정보 포함 - 팔로우 정보가 정확히 포함되는지 확인")
    void getPostList_WithFollowInfo_OtherUserPost_Success() {
        // given
        String postType = "PANGYO_1";
        List<Post> mockPosts = List.of(mockAdminPost); // 관리자 게시글 사용
        Map<Long, Map<String, String>> memberInfoMap = Map.of(
                ADMIN_ID, Map.of("nickname", ADMIN_NICKNAME, "imageUrl", MEMBER_PROFILE_IMG_URL)
        );
        List<Long> likedPostIds = List.of();

        given(postRepository.findTopNByBoardTypeOrderByCreatedAtDescIdDesc(PANGYO_1_BOARD_TYPE, DEFAULT_LIMIT))
                .willReturn(mockPosts);
        given(memberService.getMemberInfoMapByIds(any())).willReturn(memberInfoMap);
        given(postImageRepository.findFirstImagesByPostIds(any())).willReturn(List.of());
        given(postLikeService.findLikedPostIdsByMember(MEMBER_ID, mockPosts)).willReturn(likedPostIds);

        // 다른 사용자의 게시글이므로 follower와 following이 다른 객체
        given(entityManager.getReference(Member.class, MEMBER_ID)).willReturn(mockMember);
        given(entityManager.getReference(Member.class, ADMIN_ID)).willReturn(mockFollowing);
        given(followRepository.existsByFollowerUserAndFollowingUser(mockMember, mockFollowing)).willReturn(true);

        // when
        PostResponseDto.PostListResponse result =
                postService.getPostList(postType, DEFAULT_LIMIT, null, MEMBER_ID);

        // then
        assertThat(result).isNotNull();
        assertThat(result.data()).hasSize(1);

        PostResponseDto.PostListItem firstItem = result.data().get(0);
        assertThat(firstItem.user().isFollowing()).isTrue();
        assertThat(firstItem.isMine()).isFalse();

        verify(postLikeService).findLikedPostIdsByMember(MEMBER_ID, mockPosts);
        verify(followRepository).existsByFollowerUserAndFollowingUser(mockMember, mockFollowing);
    }

    @Test
    @DisplayName("잘못된 limit 값 - PostException이 발생하는지 확인")
    void getPostList_InvalidLimit_ThrowsException() {
        // given
        String postType = "PANGYO_1";
        int invalidLimit = 0;

        // when & then
        assertThatThrownBy(() ->
                postService.getPostList(postType, invalidLimit, null, null))
                .isInstanceOf(PostException.class)
                .satisfies(exception -> {
                    PostException postException = (PostException) exception;
                    assertThat(postException.getErrorCode()).isEqualTo(GeneralErrorCode.INVALID_QUERY_PARAMETER);
                });
    }

    // ========== deletePost() 메서드 테스트 ==========

    @Test
    @DisplayName("본인 게시글 삭제 - 정상적으로 삭제되는지 확인")
    void deletePost_OwnPost_Success() {
        // given
        Long postId = POST_ID;
        given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));

        // when
        postService.deletePost(postId, MEMBER_ID);

        // then
        verify(postRepository).findById(postId);
        verify(postRepository).delete(mockPost);
    }

    @Test
    @DisplayName("존재하지 않는 게시글 삭제 - PostException이 발생하는지 확인")
    void deletePost_PostNotFound_ThrowsException() {
        // given
        Long nonExistentPostId = NON_EXISTENT_POST_ID;
        given(postRepository.findById(nonExistentPostId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                postService.deletePost(nonExistentPostId, MEMBER_ID))
                .isInstanceOf(PostException.class)
                .satisfies(exception -> {
                    PostException postException = (PostException) exception;
                    assertThat(postException.getErrorCode()).isEqualTo(GeneralErrorCode.RESOURCE_NOT_FOUND);
                });

        verify(postRepository).findById(nonExistentPostId);
        verify(postRepository, never()).delete(any());
    }

    // ========== findById() 메서드 테스트 ==========

    @Test
    @DisplayName("유효한 ID로 게시글 조회 - 정상적으로 게시글이 반환되는지 확인")
    void findById_ValidId_Success() {
        // given
        Long postId = POST_ID;
        given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));

        // when
        Post result = postService.findById(postId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(mockPost.getId());
        assertThat(result.getMemberId()).isEqualTo(MEMBER_ID);

        verify(postRepository).findById(postId);
    }

    @Test
    @DisplayName("존재하지 않는 ID - PostException이 발생하는지 확인")
    void findById_InvalidId_ThrowsException() {
        // given
        Long nonExistentPostId = NON_EXISTENT_POST_ID;
        given(postRepository.findById(nonExistentPostId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.findById(nonExistentPostId))
                .isInstanceOf(PostException.class)
                .satisfies(exception -> {
                    PostException postException = (PostException) exception;
                    assertThat(postException.getErrorCode()).isEqualTo(GeneralErrorCode.RESOURCE_NOT_FOUND);
                });

        verify(postRepository).findById(nonExistentPostId);
    }

    // ========== summarizeYoutube() 메서드 테스트 ==========

    @Test
    @DisplayName("유효한 요약 반환 - 정상적으로 요약이 반환되는지 확인")
    void summarizeYoutube_ValidSummary_Success() {
        // given
        Long postId = POST_ID;
        Post postWithSummary = PostFixture.createPostWithYoutubeSummary();

        given(postRepository.findById(postId)).willReturn(Optional.of(postWithSummary));

        // when
        PostResponseDto.YouTubeSummaryResponse result =
                postService.summarizeYoutube(postId, MEMBER_ID);

        // then
        assertThat(result).isNotNull();
        assertThat(result.summary()).isEqualTo(POST_YOUTUBE_SUMMARY);

        verify(postRepository).findById(postId);
    }

    @Test
    @DisplayName("게시글 없음 - PostException이 발생하는지 확인")
    void summarizeYoutube_PostNotFound_ThrowsException() {
        // given
        Long nonExistentPostId = NON_EXISTENT_POST_ID;
        given(postRepository.findById(nonExistentPostId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                postService.summarizeYoutube(nonExistentPostId, MEMBER_ID))
                .isInstanceOf(PostException.class)
                .satisfies(exception -> {
                    PostException postException = (PostException) exception;
                    assertThat(postException.getErrorCode()).isEqualTo(GeneralErrorCode.RESOURCE_NOT_FOUND);
                });

        verify(postRepository).findById(nonExistentPostId);
    }

    // ========== Follow 관련 추가 테스트 ==========

    @Test
    @DisplayName("팔로우 상태 확인 - EntityManager와 FollowRepository 상호작용 테스트")
    void getPostDetail_FollowStatusCheck_Success() {
        // given
        Long postId = ADMIN_POST_ID;
        Map<String, String> memberInfo = Map.of(
                "nickname", ADMIN_NICKNAME,
                "imageUrl", MEMBER_PROFILE_IMG_URL
        );

        given(postRepository.findById(postId)).willReturn(Optional.of(mockAdminPost));
        given(memberService.getMemberInfo(ADMIN_ID)).willReturn(memberInfo);
        given(postLikeService.isLikedByMember(postId, MEMBER_ID)).willReturn(false);
        given(postImageRepository.findByPostIdOrderBySortIndexAsc(postId)).willReturn(List.of());

        // EntityManager Mock 설정
        given(entityManager.getReference(Member.class, MEMBER_ID)).willReturn(mockMember);
        given(entityManager.getReference(Member.class, ADMIN_ID)).willReturn(mockFollowing);

        // 팔로우 상태 Mock 설정
        given(followRepository.existsByFollowerUserAndFollowingUser(mockMember, mockFollowing)).willReturn(true);

        // when
        PostResponseDto.PostDetailResponse result = postService.getPostDetail(postId, MEMBER_ID);

        // then
        assertThat(result).isNotNull();
        assertThat(result.data().isMine()).isFalse();

        // EntityManager 호출 검증
        verify(entityManager).getReference(Member.class, MEMBER_ID);
        verify(entityManager).getReference(Member.class, ADMIN_ID);

        // FollowRepository 호출 검증
        verify(followRepository).existsByFollowerUserAndFollowingUser(mockMember, mockFollowing);
    }

    @Test
    @DisplayName("게시글 목록에서 팔로우 상태 확인 - createPostListItem 메서드 테스트")
    void getPostList_FollowStatusInList_Success() {
        // given
        String postType = "PANGYO_1";
        List<Post> mockPosts = List.of(mockPost);
        Map<Long, Map<String, String>> memberInfoMap = Map.of(
                MEMBER_ID, Map.of("nickname", MEMBER_NICKNAME, "imageUrl", MEMBER_PROFILE_IMG_URL)
        );
        List<Long> likedPostIds = List.of();

        given(postRepository.findTopNByBoardTypeOrderByCreatedAtDescIdDesc(PANGYO_1_BOARD_TYPE, DEFAULT_LIMIT))
                .willReturn(mockPosts);
        given(memberService.getMemberInfoMapByIds(any())).willReturn(memberInfoMap);
        given(postImageRepository.findFirstImagesByPostIds(any())).willReturn(List.of());
        given(postLikeService.findLikedPostIdsByMember(ADMIN_ID, mockPosts)).willReturn(likedPostIds);

        // EntityManager와 FollowRepository Mock 설정
        given(entityManager.getReference(Member.class, ADMIN_ID)).willReturn(mockMember);
        given(entityManager.getReference(Member.class, MEMBER_ID)).willReturn(mockFollowing);
        given(followRepository.existsByFollowerUserAndFollowingUser(mockMember, mockFollowing)).willReturn(false);

        // when
        PostResponseDto.PostListResponse result =
                postService.getPostList(postType, DEFAULT_LIMIT, null, ADMIN_ID);

        // then
        assertThat(result).isNotNull();
        assertThat(result.data()).hasSize(1);

        // PostListItem의 UserInfo에서 isFollowing 확인
        PostResponseDto.PostListItem firstItem = result.data().get(0);
        assertThat(firstItem.user().isFollowing()).isFalse();

        // Repository 메서드들이 올바르게 호출되었는지 확인
        verify(followRepository).existsByFollowerUserAndFollowingUser(mockMember, mockFollowing);
        verify(entityManager, times(2)).getReference(eq(Member.class), any(Long.class));
    }

    @Test
    @DisplayName("자기 자신의 게시글 목록 조회 - 팔로우 상태는 확인하지만 의미없음")
    void getPostList_OwnPostsFollowCheck_Success() {
        // given
        String postType = "PANGYO_1";
        List<Post> mockPosts = List.of(mockPost);
        Map<Long, Map<String, String>> memberInfoMap = Map.of(
                MEMBER_ID, Map.of("nickname", MEMBER_NICKNAME, "imageUrl", MEMBER_PROFILE_IMG_URL)
        );
        List<Long> likedPostIds = List.of();

        given(postRepository.findTopNByBoardTypeOrderByCreatedAtDescIdDesc(PANGYO_1_BOARD_TYPE, DEFAULT_LIMIT))
                .willReturn(mockPosts);
        given(memberService.getMemberInfoMapByIds(any())).willReturn(memberInfoMap);
        given(postImageRepository.findFirstImagesByPostIds(any())).willReturn(List.of());
        given(postLikeService.findLikedPostIdsByMember(MEMBER_ID, mockPosts)).willReturn(likedPostIds);

        // 자기 자신에 대한 EntityManager 설정
        given(entityManager.getReference(Member.class, MEMBER_ID)).willReturn(mockMember);
        given(followRepository.existsByFollowerUserAndFollowingUser(mockMember, mockMember)).willReturn(false);

        // when
        PostResponseDto.PostListResponse result =
                postService.getPostList(postType, DEFAULT_LIMIT, null, MEMBER_ID);

        // then
        assertThat(result).isNotNull();
        assertThat(result.data()).hasSize(1);

        PostResponseDto.PostListItem firstItem = result.data().get(0);
        // 자기 자신의 게시글이므로 isMine은 true이지만, 팔로우 체크는 여전히 수행됨
        assertThat(firstItem.isMine()).isTrue();

        verify(followRepository).existsByFollowerUserAndFollowingUser(any(Member.class), any(Member.class));
    }

    // ========== getMemberInfoByPosts() 메서드 테스트 ==========

    @Test
    @DisplayName("게시글 목록으로부터 회원 정보 조회 - 정상적으로 회원 정보가 반환되는지 확인")
    void getMemberInfoByPosts_Success() {
        // given
        List<Post> posts = List.of(mockPost, mockAdminPost);
        Map<Long, Map<String, String>> expectedMemberInfoMap = Map.of(
                MEMBER_ID, Map.of("nickname", MEMBER_NICKNAME, "imageUrl", MEMBER_PROFILE_IMG_URL),
                ADMIN_ID, Map.of("nickname", ADMIN_NICKNAME, "imageUrl", MEMBER_PROFILE_IMG_URL)
        );

        given(memberService.getMemberInfoMapByIds(any())).willReturn(expectedMemberInfoMap);

        // when
        Map<Long, Map<String, String>> result = postService.getMemberInfoByPosts(posts);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(MEMBER_ID)).containsEntry("nickname", MEMBER_NICKNAME);
        assertThat(result.get(ADMIN_ID)).containsEntry("nickname", ADMIN_NICKNAME);

        verify(memberService).getMemberInfoMapByIds(any());
    }

    @Test
    @DisplayName("빈 게시글 목록으로 회원 정보 조회 - 빈 맵이 반환되는지 확인")
    void getMemberInfoByPosts_EmptyList_Success() {
        // given
        List<Post> emptyPosts = List.of();

        // when
        Map<Long, Map<String, String>> result = postService.getMemberInfoByPosts(emptyPosts);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(memberService, never()).getMemberInfoMapByIds(any());
    }

    // ========== findFirstImageUrlsByPosts() 메서드 테스트 ==========

    @Test
    @DisplayName("게시글 목록의 첫 번째 이미지 URL 조회 - 정상적으로 이미지 URL이 반환되는지 확인")
    void findFirstImageUrlsByPosts_Success() {
        // given
        List<Post> posts = List.of(mockPost);
        List<PostImage> firstImages = List.of(PostImageFixture.createBasicPostImage(mockPost));

        given(postImageRepository.findFirstImagesByPostIds(any())).willReturn(firstImages);

        // when
        Map<Long, String> result = postService.findFirstImageUrlsByPosts(posts);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        verify(postImageRepository).findFirstImagesByPostIds(any());
    }

    @Test
    @DisplayName("빈 게시글 목록으로 이미지 URL 조회 - 빈 맵이 반환되는지 확인")
    void findFirstImageUrlsByPosts_EmptyList_Success() {
        // given
        List<Post> emptyPosts = List.of();

        // when
        Map<Long, String> result = postService.findFirstImageUrlsByPosts(emptyPosts);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(postImageRepository, never()).findFirstImagesByPostIds(any());
    }
}