package com.nudgeup.backend.global.exception;

import com.nudgeup.backend.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// TODO: 전역 예외 처리 핸들러
// - CustomException → ErrorCode의 status + message로 응답
// - MethodArgumentNotValidException → @Valid 실패 시 400 응답 (필드별 메시지 추출)
// - Exception → 500 응답
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        // TODO: 구현
        return null;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        // TODO: 구현
        return null;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        // TODO: 구현
        return null;
    }
}
