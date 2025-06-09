package com.kakaobase.snsapp.domain.comments.converter;

import com.kakaobase.snsapp.domain.comments.dto.BotRecommentRequestDto;
import com.kakaobase.snsapp.domain.comments.entity.Comment;
import com.kakaobase.snsapp.domain.comments.entity.Recomment;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.posts.entity.Post;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

public class BotRecommentConverter {

    public static BotRecommentRequestDto toRequestDto(
            Post post,
            Member postWriter,
            Comment comment,
            List<Recomment> recomments
    ) {
        var postDto = new BotRecommentRequestDto.PostDto(
                post.getId(),
                new BotRecommentRequestDto.UserDto(
                        postWriter.getNickname(),
                        postWriter.getClassName().toString()
                ),
                formatUtc(post.getCreatedAt().toInstant(ZoneOffset.UTC)),
                post.getContent()
        );

        var recommentDtos = recomments.stream()
                .map(r -> new BotRecommentRequestDto.RecommentDto(
                        new BotRecommentRequestDto.UserDto(
                                r.getMember().getNickname(),
                                r.getMember().getClassName().toString()
                        ),
                        formatUtc(r.getCreatedAt().toInstant(ZoneOffset.UTC)),
                        r.getContent()
                ))
                .toList();

        var commentDto = new BotRecommentRequestDto.CommentDto(
                comment.getId(),
                new BotRecommentRequestDto.UserDto(
                        comment.getMember().getNickname(),
                        comment.getMember().getClassName().toString()
                ),
                formatUtc(comment.getCreatedAt().toInstant(ZoneOffset.UTC)),
                comment.getContent(),
                recommentDtos
        );

        return new BotRecommentRequestDto(
                post.getBoardType().name(),
                postDto,
                commentDto
        );
    }

    private static String formatUtc(Instant instant) {
        // 마이크로초까지만 출력하고 Z 붙이기
        long seconds = instant.getEpochSecond();
        int micros = instant.getNano() / 1000;  // 나노초를 마이크로초로 변환

        // 포맷: yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'
        return String.format("%s.%06dZ",
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                        .withZone(ZoneOffset.UTC)
                        .format(instant),
                micros
        );
    }
}
