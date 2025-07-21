package com.kakaobase.snsapp.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;


@Slf4j
@Component
@RequiredArgsConstructor
public class JwtWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {

        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

            // SecurityContext에서 인증 정보만 가져오기
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()
                    && !(authentication instanceof AnonymousAuthenticationToken)) {

                // 인증된 사용자 정보를 WebSocket 세션에 저장
                attributes.put("SPRING_SECURITY_CONTEXT", new SecurityContextImpl(authentication));
                attributes.put("USER_ID", authentication.getName());

                log.debug("WebSocket 핸드셰이크 - 인증된 사용자: {}", authentication.getName());
                return true;
            }

            log.warn("WebSocket 핸드셰이크 실패 - 인증되지 않은 사용자");
        }

        return false; // 인증되지 않은 경우 연결 거부
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        if (exception == null) {
            log.debug("WebSocket 핸드셰이크 완료");
        } else {
            log.warn("WebSocket 핸드셰이크 실패: {}", exception.getMessage());
        }
    }
}
