package com.nudgeup.backend.global.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// TODO: 모든 요청에서 Authorization 헤더를 검사하는 JWT 필터
// - "Bearer {token}" 형식에서 토큰 추출
// - JwtProvider.validateToken()으로 유효성 검사
// - 유효하면 SecurityContextHolder에 Authentication 저장
// - SecurityConfig에서 UsernamePasswordAuthenticationFilter 앞에 등록
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // TODO: 구현
        // 1. request.getHeader("Authorization") 에서 토큰 추출
        // 2. jwtProvider.validateToken(token) 검증
        // 3. SecurityContextHolder.getContext().setAuthentication(authentication) 설정
        filterChain.doFilter(request, response);
    }
}
