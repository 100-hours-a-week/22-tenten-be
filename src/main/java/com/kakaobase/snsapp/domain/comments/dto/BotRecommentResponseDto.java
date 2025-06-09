package com.kakaobase.snsapp.domain.comments.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BotRecommentResponseDto {

    private Data data;

    @Getter
    @NoArgsConstructor
    public static class Data {
        private String content;
    }
}
