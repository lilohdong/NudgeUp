package com.nudgeup.backend.global.security;

import com.nudgeup.backend.domain.user.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

// TODO: Spring Security가 인증 처리에 사용하는 UserDetails 구현체
// - User 엔티티를 감싸는 래퍼 역할
// - getUsername()은 userId를 String으로 반환 (JwtProvider와 일관성 유지)
@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        // TODO: user.getPassword() 반환
        return null;
    }

    @Override
    public String getUsername() {
        // TODO: user.getId().toString() 반환
        return null;
    }
}
