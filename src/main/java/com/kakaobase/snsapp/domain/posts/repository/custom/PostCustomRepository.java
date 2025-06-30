package com.kakaobase.snsapp.domain.posts.repository.custom;

import com.kakaobase.snsapp.domain.posts.dto.PostResponseDto;
import com.kakaobase.snsapp.domain.posts.util.BoardType;

import java.util.List;
import java.util.Optional;

public interface PostCustomRepository {

    Optional<PostResponseDto.PostDetails> findPostDetailById(Long postId, Long memberId);

    List<PostResponseDto.PostDetails> findByBoardTypeWithCursor(
            BoardType boardType,
            Long cursor,
            int limit,
            Long memberId);

    List<PostResponseDto.PostDetails> findByMemberWithCursor(
            Long memberId,
            Long cursor,
            int limit,
            Long currentMemberId);

    List<PostResponseDto.PostDetails> findLikedPostsWithCursor(
            Long likedByMemberId,
            Long cursor,
            int limit,
            Long currentMemberId);

    void deletePost(Long postId);
}
