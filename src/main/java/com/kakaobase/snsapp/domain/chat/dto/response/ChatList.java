package com.kakaobase.snsapp.domain.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ChatList(
   List<ChatItemDto> chats,
   @JsonProperty("has_next")
   Boolean hasNext
) {}
