package com.nudgeup.backend.global.security;

import com.nudgeup.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// TODO: userId(String)로 User를 조회해 CustomUserDetails로 감싸 반환
// - JwtFilter에서 getAuthentication() 호출 시 내부적으로 사용됨
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        // TODO: userRepository.findById(Long.parseLong(userId))로 User 조회
        // - 없으면 ErrorCode.USER_NOT_FOUND throw
        // - 있으면 new CustomUserDetails(user) 반환
        return null;
    }
}
