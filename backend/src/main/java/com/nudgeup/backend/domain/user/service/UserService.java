package com.nudgeup.backend.domain.user.service;

import com.nudgeup.backend.domain.user.dto.request.FcmTokenRequest;
import com.nudgeup.backend.domain.user.dto.request.UpdateColorRequest;
import com.nudgeup.backend.domain.user.dto.request.UpdateUserRequest;
import com.nudgeup.backend.domain.user.dto.response.UserResponse;
import com.nudgeup.backend.domain.user.repository.UserRepository;
import com.nudgeup.backend.global.exception.CustomException;
import com.nudgeup.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getMyProfile(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public UserResponse updateMyUsername(Long userId, UpdateUserRequest request) {
        return userRepository.findById(userId)
                .map(user -> {
                    if (request.getUsername() != null) {
                        user.updateUsername(request.getUsername());
                    }
                    return UserResponse.from(user);
                })
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public UserResponse updateMyColor(Long userId, UpdateColorRequest request) {
        return userRepository.findById(userId)
                .map(user -> {
                    user.updateColor(request.getColor());
                    return UserResponse.from(user);
                })
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public void updateFcmToken(Long userId, FcmTokenRequest request) {
        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND))
                .updateFcmToken(request.getFcmToken());
    }
}
