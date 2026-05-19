package com.nudgeup.backend.domain.user.service;

import com.nudgeup.backend.domain.user.dto.request.FcmTokenRequest;
import com.nudgeup.backend.domain.user.dto.request.UpdateUserRequest;
import com.nudgeup.backend.domain.user.dto.response.UserResponse;
import com.nudgeup.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// TODO: 사용자 프로필 조회/수정, FCM 토큰 등록 비즈니스 로직
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getMyProfile(Long userId) {
        // TODO: userRepository.findById(userId) 조회 후 UserResponse.from() 반환
        return null;
    }

    @Transactional
    public UserResponse updateMyProfile(Long userId, UpdateUserRequest request) {
        // TODO: 조회 → user.updateProfile() 호출 → UserResponse.from() 반환
        return null;
    }

    @Transactional
    public void updateFcmToken(Long userId, FcmTokenRequest request) {
        // TODO: 조회 → user.updateFcmToken() 호출
    }
}
