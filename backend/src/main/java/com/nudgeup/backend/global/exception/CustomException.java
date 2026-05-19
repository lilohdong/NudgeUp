package com.nudgeup.backend.global.exception;

import lombok.Getter;

// TODO: ErrorCode를 담아 던지는 커스텀 런타임 예외
// - Service 레이어에서 비즈니스 규칙 위반 시 throw
// - GlobalExceptionHandler에서 catch하여 ApiResponse.fail()로 응답
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
