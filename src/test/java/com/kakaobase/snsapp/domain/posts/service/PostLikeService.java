package com.kakaobase.snsapp.domain.posts.service;

import com.kakaobase.snsapp.domain.members.service.MemberService;
import com.kakaobase.snsapp.domain.posts.entity.Post;
import com.kakaobase.snsapp.domain.posts.entity.PostLike;
import com.kakaobase.snsapp.domain.posts.exception.PostErrorCode;
import com.kakaobase.snsapp.domain.posts.exception.PostException;
import com.kakaobase.snsapp.domain.posts.repository.PostLikeRepository;
import com.kakaobase.snsapp.domain.posts.repository.PostRepository;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import com.kakaobase.snsapp.global.fixture.PostFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.kakaobase.snsapp.global.constants.MemberFixtureConstants.*;
import static com.kakaobase.snsapp.global.constants.PostFixtureConstants.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostLikeService 단위 테스트")
@Transactional
class PostLikeServiceTest {

    @InjectMocks
    private PostLikeService postLikeService;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private MemberService memberService;

    private Post mockPost;
    private Post mockAdminPost;

    @BeforeEach
    void setUp() {
        // Mock 게시글 생성
        mockPost = PostFixture.createKbtPost();
        ReflectionTestUtils.setField(mockPost, "id", POST_ID);

        mockAdminPost = PostFixture.createAdminPost();
        ReflectionTestUtils.setField(mockAdminPost, "id", ADMIN_POST_ID);
    }

    // ========== addLike() 메서드 테스트 ==========

    @Test
    @DisplayName("게시글 좋아요 추가 - 정상적으로 좋아요가 추가되는지 확인")
    void addLike_Success() {
        // given
        Long postId = POST_ID;
        Long memberId = MEMBER_ID;

        given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));
        given(postLikeRepository.existsByMemberIdAndPostId(memberId, postId)).willReturn(false);

        // when
        postLikeService.addLike(postId, memberId);

        // then
        verify(postRepository).findById(postId);
        verify(postLikeRepository).existsByMemberIdAndPostId(memberId, postId);
        verify(postLikeRepository).save(any(PostLike.class));
        verify(postRepository).increaseLikeCount(postId);
    }

    @Test
    @DisplayName("다른 사용자 게시글에 좋아요 추가 - 정상적으로 처리되는지 확인")
    void addLike_OtherUserPost_Success() {
        // given
        Long postId = ADMIN_POST_ID;
        Long memberId = MEMBER_ID;

        given(postRepository.findById(postId)).willReturn(Optional.of(mockAdminPost));
        given(postLikeRepository.existsByMemberIdAndPostId(memberId, postId)).willReturn(false);

        // when
        postLikeService.addLike(postId, memberId);

        // then
        verify(postRepository).findById(postId);
        verify(postLikeRepository).existsByMemberIdAndPostId(memberId, postId);
        verify(postLikeRepository).save(any(PostLike.class));
        verify(postRepository).increaseLikeCount(postId);
    }

    @Test
    @DisplayName("존재하지 않는 게시글에 좋아요 - PostException이 발생하는지 확인")
    void addLike_PostNotFound_ThrowsException() {
        // given
        Long nonExistentPostId = NON_EXISTENT_POST_ID;
        Long memberId = MEMBER_ID;

        given(postRepository.findById(nonExistentPostId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postLikeService.addLike(nonExistentPostId, memberId))
                .isInstanceOf(PostException.class)
                .satisfies(exception -> {
                    PostException postException = (PostException) exception;
                    assertThat(postException.getErrorCode()).isEqualTo(GeneralErrorCode.RESOURCE_NOT_FOUND);
                    assertThat(postException.getEffectiveField()).isEqualTo("postId");
                });

        verify(postRepository).findById(nonExistentPostId);
        verify(postLikeRepository, never()).save(any());
        verify(postRepository, never()).increaseLikeCount(any());
    }

    @Test
    @DisplayName("이미 좋아요한 게시글에 재좋아요 - PostException이 발생하는지 확인")
    void addLike_AlreadyLiked_ThrowsException() {
        // given
        Long postId = POST_ID;
        Long memberId = MEMBER_ID;

        given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));
        given(postLikeRepository.existsByMemberIdAndPostId(memberId, postId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> postLikeService.addLike(postId, memberId))
                .isInstanceOf(PostException.class)
                .satisfies(exception -> {
                    PostException postException = (PostException) exception;
                    assertThat(postException.getErrorCode()).isEqualTo(PostErrorCode.ALREADY_LIKED);
                });

        verify(postRepository).findById(postId);
        verify(postLikeRepository).existsByMemberIdAndPostId(memberId, postId);
        verify(postLikeRepository, never()).save(any());
        verify(postRepository, never()).increaseLikeCount(any());
    }

    // ========== removeLike() 메서드 테스트 ==========

    @Test
    @DisplayName("게시글 좋아요 취소 - 정상적으로 좋아요가 취소되는지 확인")
    void removeLike_Success() {
        // given
        Long postId = POST_ID;
        Long memberId = MEMBER_ID;
        PostLike mockPostLike = new PostLike(memberId, postId);

        given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));
        given(postLikeRepository.findByMemberIdAndPostId(memberId, postId))
                .willReturn(Optional.of(mockPostLike));

        // when
        postLikeService.removeLike(postId, memberId);

        // then
        verify(postRepository).findById(postId);
        verify(postLikeRepository).findByMemberIdAndPostId(memberId, postId);
        verify(postLikeRepository).delete(mockPostLike);
        verify(postRepository).decreaseLikeCount(postId);
    }

    @Test
    @DisplayName("다른 사용자 게시글 좋아요 취소 - 정상적으로 처리되는지 확인")
    void removeLike_OtherUserPost_Success() {
        // given
        Long postId = ADMIN_POST_ID;
        Long memberId = MEMBER_ID;
        PostLike mockPostLike = new PostLike(memberId, postId);

        given(postRepository.findById(postId)).willReturn(Optional.of(mockAdminPost));
        given(postLikeRepository.findByMemberIdAndPostId(memberId, postId))
                .willReturn(Optional.of(mockPostLike));

        // when
        postLikeService.removeLike(postId, memberId);

        // then
        verify(postRepository).findById(postId);
        verify(postLikeRepository).findByMemberIdAndPostId(memberId, postId);
        verify(postLikeRepository).delete(mockPostLike);
        verify(postRepository).decreaseLikeCount(postId);
    }

    @Test
    @DisplayName("존재하지 않는 게시글 좋아요 취소 - PostException이 발생하는지 확인")
    void removeLike_PostNotFound_ThrowsException() {
        // given
        Long nonExistentPostId = NON_EXISTENT_POST_ID;
        Long memberId = MEMBER_ID;

        given(postRepository.findById(nonExistentPostId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postLikeService.removeLike(nonExistentPostId, memberId))
                .isInstanceOf(PostException.class)
                .satisfies(exception -> {
                    PostException postException = (PostException) exception;
                    assertThat(postException.getErrorCode()).isEqualTo(GeneralErrorCode.RESOURCE_NOT_FOUND);
                    assertThat(postException.getEffectiveField()).isEqualTo("postId");
                });

        verify(postRepository).findById(nonExistentPostId);
        verify(postLikeRepository, never()).delete(any());
        verify(postRepository, never()).decreaseLikeCount(any());
    }

    @Test
    @DisplayName("좋아요하지 않은 게시글 취소 시도 - PostException이 발생하는지 확인")
    void removeLike_NotLiked_ThrowsException() {
        // given
        Long postId = POST_ID;
        Long memberId = MEMBER_ID;

        given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));
        given(postLikeRepository.findByMemberIdAndPostId(memberId, postId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postLikeService.removeLike(postId, memberId))
                .isInstanceOf(PostException.class)
                .satisfies(exception -> {
                    PostException postException = (PostException) exception;
                    assertThat(postException.getErrorCode()).isEqualTo(PostErrorCode.ALREADY_UNLIKED);
                });

        verify(postRepository).findById(postId);
        verify(postLikeRepository).findByMemberIdAndPostId(memberId, postId);
        verify(postLikeRepository, never()).delete(any());
        verify(postRepository, never()).decreaseLikeCount(any());
    }

    // ========== isLikedByMember() 메서드 테스트 ==========

    @Test
    @DisplayName("좋아요 여부 확인 - 좋아요한 경우 true 반환")
    void isLikedByMember_Liked_ReturnsTrue() {
        // given
        Long postId = POST_ID;
        Long memberId = MEMBER_ID;

        given(postLikeRepository.existsByMemberIdAndPostId(memberId, postId)).willReturn(true);

        // when
        boolean result = postLikeService.isLikedByMember(postId, memberId);

        // then
        assertThat(result).isTrue();
        verify(postLikeRepository).existsByMemberIdAndPostId(memberId, postId);
    }

    @Test
    @DisplayName("좋아요 여부 확인 - 좋아요하지 않은 경우 false 반환")
    void isLikedByMember_NotLiked_ReturnsFalse() {
        // given
        Long postId = POST_ID;
        Long memberId = MEMBER_ID;

        given(postLikeRepository.existsByMemberIdAndPostId(memberId, postId)).willReturn(false);

        // when
        boolean result = postLikeService.isLikedByMember(postId, memberId);

        // then
        assertThat(result).isFalse();
        verify(postLikeRepository).existsByMemberIdAndPostId(memberId, postId);
    }

    // ========== findLikedPostIdsByMember() 메서드 테스트 ==========

    @Test
    @DisplayName("회원이 좋아요한 게시글 ID 목록 조회 - 정상적으로 목록이 반환되는지 확인")
    void findLikedPostIdsByMember_Success() {
        // given
        Long memberId = MEMBER_ID;
        List<Long> expectedPostIds = List.of(POST_ID, ADMIN_POST_ID);

        given(postLikeRepository.findPostIdsByMemberId(memberId)).willReturn(expectedPostIds);

        // when
        List<Long> result = postLikeService.findLikedPostIdsByMember(memberId);

        // then
        assertThat(result).isEqualTo(expectedPostIds);
        assertThat(result).hasSize(2);
        verify(postLikeRepository).findPostIdsByMemberId(memberId);
    }

    @Test
    @DisplayName("좋아요한 게시글이 없는 경우 - 빈 목록 반환")
    void findLikedPostIdsByMember_NoLikes_ReturnsEmptyList() {
        // given
        Long memberId = MEMBER_ID;
        List<Long> emptyList = List.of();

        given(postLikeRepository.findPostIdsByMemberId(memberId)).willReturn(emptyList);

        // when
        List<Long> result = postLikeService.findLikedPostIdsByMember(memberId);

        // then
        assertThat(result).isEmpty();
        verify(postLikeRepository).findPostIdsByMemberId(memberId);
    }

    // ========== findLikedPostIdsByMember(memberId, posts) 메서드 테스트 ==========

    @Test
    @DisplayName("게시글 목록 중 좋아요한 게시글 조회 - 정상적으로 필터링되는지 확인")
    void findLikedPostIdsByMember_WithPosts_Success() {
        // given
        Long memberId = MEMBER_ID;
        List<Post> posts = List.of(mockPost, mockAdminPost);
        List<Long> postIds = List.of(POST_ID, ADMIN_POST_ID);
        List<Long> likedPostIds = List.of(POST_ID);

        given(postLikeRepository.findPostIdsByMemberIdAndPostIdIn(memberId, postIds))
                .willReturn(likedPostIds);

        // when
        List<Long> result = postLikeService.findLikedPostIdsByMember(memberId, posts);

        // then
        assertThat(result).isEqualTo(likedPostIds);
        assertThat(result).hasSize(1);
        assertThat(result).contains(POST_ID);
        verify(postLikeRepository).findPostIdsByMemberIdAndPostIdIn(memberId, postIds);
    }

    @Test
    @DisplayName("빈 게시글 목록으로 조회 - 빈 목록 반환")
    void findLikedPostIdsByMember_EmptyPosts_ReturnsEmptyList() {
        // given
        Long memberId = MEMBER_ID;
        List<Post> emptyPosts = List.of();

        // when
        List<Long> result = postLikeService.findLikedPostIdsByMember(memberId, emptyPosts);

        // then
        assertThat(result).isEmpty();
        verify(postLikeRepository, never()).findPostIdsByMemberIdAndPostIdIn(any(), any());
    }

    @Test
    @DisplayName("게시글 목록 중 좋아요하지 않은 경우 - 빈 목록 반환")
    void findLikedPostIdsByMember_NoLikesInPosts_ReturnsEmptyList() {
        // given
        Long memberId = MEMBER_ID;
        List<Post> posts = List.of(mockPost, mockAdminPost);
        List<Long> postIds = List.of(POST_ID, ADMIN_POST_ID);
        List<Long> emptyLikedList = List.of();

        given(postLikeRepository.findPostIdsByMemberIdAndPostIdIn(memberId, postIds))
                .willReturn(emptyLikedList);

        // when
        List<Long> result = postLikeService.findLikedPostIdsByMember(memberId, posts);

        // then
        assertThat(result).isEmpty();
        verify(postLikeRepository).findPostIdsByMemberIdAndPostIdIn(memberId, postIds);
    }

    // ========== deleteAllByPostId() 메서드 테스트 ==========

    @Test
    @DisplayName("게시글 관련 좋아요 일괄 삭제 - 정상적으로 삭제되는지 확인")
    void deleteAllByPostId_Success() {
        // given
        Long postId = POST_ID;

        // when
        postLikeService.deleteAllByPostId(postId);

        // then
        verify(postLikeRepository).deleteByPostId(postId);
    }

    @Test
    @DisplayName("좋아요가 없는 게시글 삭제 - 정상적으로 처리되는지 확인")
    void deleteAllByPostId_NoLikes_Success() {
        // given
        Long postId = NON_EXISTENT_POST_ID;

        // when
        postLikeService.deleteAllByPostId(postId);

        // then
        verify(postLikeRepository).deleteByPostId(postId);
    }
}