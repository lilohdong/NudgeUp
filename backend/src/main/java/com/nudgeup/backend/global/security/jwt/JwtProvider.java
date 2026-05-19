package com.nudgeup.backend.global.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

// TODO: JWT 토큰 생성 / 파싱 / 검증 담당
// - generateAccessToken(userId): access token 생성 (claim에 userId 포함)
// - generateRefreshToken(userId): refresh token 생성
// - getAuthentication(token): 토큰에서 Authentication 객체 추출
// - validateToken(token): 유효성 검증 (만료/위변조 여부)
// - getUserId(token): 토큰 claim에서 userId 추출
// 의존성: io.jsonwebtoken:jjwt-api (build.gradle에 이미 추가됨)
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtProperties jwtProperties;

    public String generateAccessToken(Long userId) {
        // TODO: 구현
        return null;
    }

    public String generateRefreshToken(Long userId) {
        // TODO: 구현
        return null;
    }

    public Authentication getAuthentication(String token) {
        // TODO: 구현
        return null;
    }

    public boolean validateToken(String token) {
        // TODO: 구현
        return false;
    }

    public Long getUserId(String token) {
        // TODO: 구현
        return null;
    }
}
