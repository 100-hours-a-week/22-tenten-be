package com.kakaobase.snsapp.domain.posts.repository.custom;

import com.kakaobase.snsapp.domain.posts.dto.PostResponseDto;
import com.kakaobase.snsapp.domain.posts.entity.Post;

import java.util.List;

public interface PostCustomRepository {

    List<PostResponseDto.PostDetails> findByBoardTypeWithCursor(
            Post.BoardType boardType,
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
}
