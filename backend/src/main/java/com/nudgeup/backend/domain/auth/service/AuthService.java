package com.nudgeup.backend.domain.auth.service;

import com.nudgeup.backend.domain.auth.dto.request.LoginRequest;
import com.nudgeup.backend.domain.auth.dto.request.RegisterRequest;
import com.nudgeup.backend.domain.auth.dto.request.TokenRefreshRequest;
import com.nudgeup.backend.domain.auth.dto.response.LoginResponse;
import com.nudgeup.backend.domain.auth.dto.response.TokenResponse;
import com.nudgeup.backend.domain.user.dto.response.UserResponse;
import com.nudgeup.backend.domain.user.entity.User;
import com.nudgeup.backend.domain.user.repository.UserRepository;
import com.nudgeup.backend.global.exception.CustomException;
import com.nudgeup.backend.global.exception.ErrorCode;
import com.nudgeup.backend.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// TODO: 인증 관련 핵심 비즈니스 로직
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        // TODO: 구현
        // 1. userRepository.existsByEmail() → 중복이면 ErrorCode.DUPLICATE_EMAIL throw
        // 2. passwordEncoder.encode(request.getPassword())
        // 3. User.builder()...build() → userRepository.save()
        // 4. UserResponse.from(user) 반환
        return null;
    }

    public LoginResponse login(LoginRequest request) {
        // TODO: 구현
        // 1. userRepository.findByEmail() → 없으면 ErrorCode.INVALID_CREDENTIALS throw
        // 2. passwordEncoder.matches() → 불일치면 ErrorCode.INVALID_CREDENTIALS throw
        // 3. jwtProvider.generateAccessToken(), generateRefreshToken()
        // 4. LoginResponse 반환
        return null;
    }

    public TokenResponse refresh(TokenRefreshRequest request) {
        // TODO: 구현
        // 1. jwtProvider.validateToken() → 유효하지 않으면 ErrorCode.INVALID_TOKEN throw
        // 2. jwtProvider.getUserId()로 userId 추출
        // 3. 새 accessToken, refreshToken 발급 후 반환
        return null;
    }

    public void logout(Long userId) {
        // TODO: RefreshToken 무효화 처리
        // 현재는 단순 클라이언트 측 토큰 삭제로 처리 (서버 저장 없음)
        // 추후 RefreshToken 블랙리스트 또는 DB 저장 방식으로 보완 가능
    }
}
