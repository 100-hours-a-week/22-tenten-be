package com.kakaobase.snsapp.global.error.handler;

import com.kakaobase.snsapp.global.common.response.CustomResponse;
import com.kakaobase.snsapp.global.error.code.BaseErrorCode;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import com.kakaobase.snsapp.global.error.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 애플리케이션 전역에서 발생하는 예외를 처리하는 클래스입니다.
 * 모든 컨트롤러에서 발생하는 예외를 일관된 형식으로 처리합니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * CustomException을 처리합니다.
     *
     * @param e CustomException
     * @return 에러 정보를 포함한 ResponseEntity
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<CustomResponse<Void>> handleCustomException(CustomException e) {
        BaseErrorCode code = e.getErrorCode();
        log.error("CustomException: {}", e.getMessage());
        return ResponseEntity
                .status(code.getStatus())
                .body(CustomResponse.failure(
                        code.getError(),
                        code.getMessage(),
                        e.getEffectiveField()
                ));
    }


    /**
     * Bean Validation 예외를 처리합니다.
     *
     * @param e MethodArgumentNotValidException
     * @return 검증 오류 정보를 포함한 ResponseEntity
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("Validation Exception: {}", e.getMessage());
        FieldError fieldError = e.getBindingResult().getFieldError();

        if (fieldError != null) {
            return ResponseEntity
                    .status(GeneralErrorCode.INVALID_FORMAT.getStatus())
                    .body(CustomResponse.failure(
                            GeneralErrorCode.INVALID_FORMAT.getError(),
                            fieldError.getDefaultMessage(),
                            fieldError.getField()
                    ));
        }

        return ResponseEntity
                .status(GeneralErrorCode.INVALID_FORMAT.getStatus())
                .body(GeneralErrorCode.INVALID_FORMAT.getErrorResponse());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CustomResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.error("HttpMessageNotReadable Exception: {}", e.getMessage());

        return ResponseEntity
                .status(GeneralErrorCode.INVALID_FORMAT.getStatus())
                .body(CustomResponse.failure(
                        GeneralErrorCode.INVALID_FORMAT.getError(),
                        GeneralErrorCode.INVALID_FORMAT.getMessage(),
                        null
                ));
    }



    /**
     * 필수 파라미터 누락 예외를 처리합니다.
     *
     * @param e MissingServletRequestParameterException
     * @return 파라미터 오류 정보를 포함한 ResponseEntity
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<CustomResponse<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e) {
        log.error("Missing Parameter Exception: {}", e.getMessage());
        return ResponseEntity
                .status(GeneralErrorCode.MISSING_REQUIRED_FIELD.getStatus())
                .body(CustomResponse.failure(
                        GeneralErrorCode.MISSING_REQUIRED_FIELD.getError(),
                        GeneralErrorCode.MISSING_REQUIRED_FIELD.getMessage(),
                        e.getParameterName()
                ));
    }

    /**
     * 바인딩 예외를 처리합니다.
     *
     * @param e BindException
     * @return 바인딩 오류 정보를 포함한 ResponseEntity
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<CustomResponse<Void>> handleBindException(BindException e) {
        log.error("Bind Exception: {}", e.getMessage());
        FieldError fieldError = e.getBindingResult().getFieldError();

        if (fieldError != null) {
            return ResponseEntity
                    .status(GeneralErrorCode.INVALID_QUERY_PARAMETER.getStatus())
                    .body(CustomResponse.failure(
                            GeneralErrorCode.INVALID_QUERY_PARAMETER.getError(),
                            fieldError.getDefaultMessage(),
                            fieldError.getField()
                    ));
        }

        return ResponseEntity
                .status(GeneralErrorCode.INVALID_QUERY_PARAMETER.getStatus())
                .body(GeneralErrorCode.INVALID_QUERY_PARAMETER.getErrorResponse());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<CustomResponse<Void>> handleAuthorizationDenied(AuthorizationDeniedException e) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(CustomResponse.failure(GeneralErrorCode.FORBIDDEN.getError(), GeneralErrorCode.FORBIDDEN.getMessage()));
    }

    /**
     * 그 외 모든 예외를 처리합니다.
     *
     * @param e Exception
     * @return 서버 오류 정보를 포함한 ResponseEntity
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CustomResponse<Void>> handleException(Exception e) {
        log.error("Unhandled Exception: ", e);
        return ResponseEntity
                .status(GeneralErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(GeneralErrorCode.INTERNAL_SERVER_ERROR.getErrorResponse());
    }
}