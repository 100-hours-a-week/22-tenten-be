package com.kakaobase.snsapp.domain.posts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 게시글 도메인의 요청 DTO를 관리하는 통합 클래스
 */
public class PostRequestDto {

    /**
     * 게시글 생성 요청 DTO
     */
    @Schema(description = "게시글 생성 요청")
    public record PostCreateRequestDto(
            @Schema(description = "게시글 본문", example = "오늘도 열심히 개발 중입니다!", maxLength = 2000)
            @Size(max = 2000, message = "게시글 본문은 최대 2000자까지 작성할 수 있습니다.")
            String content,

            @Schema(description = "이미지 URL", example = "https://s3.amazonaws.com/bucket/uploads/dev.jpg", required = false)
            String image_url,

            @Schema(description = "유튜브 URL", example = "https://www.youtube.com/watch?v=abcd1234", required = false)
            String youtube_url
    ) {
        /**
         * 게시글 내용이 비어있는지 검증합니다.
         * 내용, 이미지, 유튜브 링크 중 하나는 반드시 존재해야 합니다.
         *
         * @return 내용이 비어있으면 true, 아니면 false
         */
        public boolean isEmpty() {
            return (content == null || content.isBlank())
                    && (image_url == null || image_url.isBlank())
                    && (youtube_url == null || youtube_url.isBlank());
        }

        /**
         * 유튜브 URL이 유효한지 검증합니다.
         *
         * @return 유효하면 true, 아니면 false
         */
        public boolean isValidYoutubeUrl() {
            if (youtube_url == null || youtube_url.trim().isEmpty()) {
                return true; // 빈 값은 유효하다고 처리
            }

            // 유튜브 URL 형식 검증 (youtube.com 또는 youtu.be 도메인 확인)
            return youtube_url.contains("youtube.com") || youtube_url.contains("youtu.be");
        }
    }

    /**
     * AI 서버 YouTube 요약 요청 DTO
     *
     * <p>AI 서버에 YouTube 영상 요약을 요청할 때 사용하는 DTO입니다.</p>
     *
     * @param url YouTube 영상 URL
     */
    @Schema(description = "AI 서버 YouTube 요약 요청")
    public record YouTubeAiRequest(
            @Schema(description = "YouTube 영상 URL", example = "https://www.youtube.com/watch?v=VIDEO_ID", required = true)
            @JsonProperty("url")
            String url
    ) {}

    /**
     * AI 서버 YouTube 요약 응답 DTO
     *
     * <p>AI 서버로부터 YouTube 영상 요약 결과를 받을 때 사용하는 DTO입니다.</p>
     *
     * @param message 응답 메시지
     * @param data 요약 데이터
     */
    @Schema(description = "AI 서버 YouTube 요약 응답")
    public record YouTubeAiResponse(
            @Schema(description = "응답 메시지", example = "YouTube 영상이 요약되었습니다.")
            String message,

            @Schema(description = "요약 데이터")
            YouTubeAiData data
    ) {
        /**
         * YouTube 요약 데이터
         *
         * @param summary 요약 내용
         */
        @Schema(description = "YouTube 요약 데이터")
        public record YouTubeAiData(
                @Schema(description = "요약 내용", example = "• 서울대 교수회가 중고교 통합과 수능 중복 응시를 포함한 교육 개혁안을 발표했습니다.\\n• 중등학교 6년제로 학재를 통합하고 초등 6년 과정은 소양 교육, 중등 6년 과정은 기초 교육 및 적성 탐색에 중점을 둬야 한다고 제안했습니다.\\n• 학생 선택권을 보장하기 위해 수능 시험을 연간 세 차례 실시하고 최고 점수나 평균 점수를 입시에 반영하는 방안을 제시했습니다.")
                String summary
        ) {}
    }
}