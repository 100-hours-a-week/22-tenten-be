package com.kakaobase.snsapp.global.error.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GeneralErrorCode implements BaseErrorCode {

    FORBIDDEN(HttpStatus.FORBIDDEN, "forbidden", "해당 자원에 접근할 권한이 없습니다", null),
    INVALID_QUERY_PARAMETER(HttpStatus.BAD_REQUEST, "invalid_query_parameter", "요청 쿼리 파라미터가 유효하지 않습니다.", null),
    MISSING_REQUIRED_FIELD(HttpStatus.BAD_REQUEST, "missing_required_field", "필수 입력 값이 누락되었습니다.", null),
    INVALID_FORMAT(HttpStatus.BAD_REQUEST, "invalid_format", "입력 형식이 잘못되었습니다.", null),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "resource_not_found", "요청한 리소스를 찾을 수 없습니다.", null),
    RESOURCE_ALREADY_EXISTS(HttpStatus.NOT_FOUND, "resource_alread_exists", "중복된 리소스 입니다.", null),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "rate_limit_exceeded", "요청이 너무 많습니다.", null),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "internal_server_error", "서버 내부 오류가 발생했습니다.", null);


    private final HttpStatus status;
    private final String error;
    private final String message;
    private final String field;

}
