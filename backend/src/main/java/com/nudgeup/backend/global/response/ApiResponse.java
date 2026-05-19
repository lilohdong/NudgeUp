package com.nudgeup.backend.global.response;

import lombok.Getter;

// TODO: 모든 API 응답을 감싸는 공통 래퍼 클래스
// - success, message, data 필드 구성
// - 정적 팩토리 메서드 success(data), success(message, data), fail(message) 구현
@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;

    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        // TODO: 구현
        return null;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        // TODO: 구현
        return null;
    }

    public static <T> ApiResponse<T> fail(String message) {
        // TODO: 구현
        return null;
    }
}
