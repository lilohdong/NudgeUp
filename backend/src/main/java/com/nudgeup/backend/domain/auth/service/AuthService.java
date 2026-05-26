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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        User user = userRepository.save(User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build());
        return UserResponse.from(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }
        return LoginResponse.builder()
                .accessToken(jwtProvider.generateAccessToken(user.getId()))
                .refreshToken(jwtProvider.generateRefreshToken(user.getId()))
                .user(UserResponse.from(user))
                .build();
    }

    public TokenResponse refresh(TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!jwtProvider.isRefreshToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        Long userId = jwtProvider.getUserId(refreshToken);
        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return TokenResponse.builder()
                .accessToken(jwtProvider.generateAccessToken(userId))
                .refreshToken(jwtProvider.generateRefreshToken(userId))
                .build();
    }

    public void logout(Long userId) {
        // 클라이언트 측 토큰 삭제로 처리
    }
}
