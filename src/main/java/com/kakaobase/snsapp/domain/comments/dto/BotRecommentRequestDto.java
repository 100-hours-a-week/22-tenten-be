package com.kakaobase.snsapp.domain.comments.dto;

import java.util.List;

public record BotRecommentRequestDto(
        String board_type,
        PostDto post,
        CommentDto comment
) {
    public record PostDto(
            Long id,
            UserDto user,
            String created_at,
            String content
    ) {}

    public record CommentDto(
            Long id,
            UserDto user,
            String created_at,
            String content,
            List<RecommentDto> recomments
    ) {}

    public record RecommentDto(
            UserDto user,
            String created_at,
            String content
    ) {}

    public record UserDto(
            String nickname,
            String class_name
    ) {}
}
