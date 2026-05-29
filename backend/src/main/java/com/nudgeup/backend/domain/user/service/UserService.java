package com.nudgeup.backend.domain.user.service;

import com.nudgeup.backend.domain.user.dto.request.FcmTokenRequest;
import java.time.LocalDateTime;
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
    public UserResponse updateMyProfile(Long userId, UpdateUserRequest request) {
        return userRepository.findById(userId)
                .map(user -> {
                    if (request.getUsername() != null) {
                        LocalDateTime lastChanged = user.getLastNameChangedAt();
                        if (lastChanged != null && LocalDateTime.now().isBefore(lastChanged.plusDays(7))) {
                            throw new CustomException(ErrorCode.NAME_CHANGE_TOO_SOON);
                        }
                    }
                    user.updateProfile(request.getUsername(), request.getColor());
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
