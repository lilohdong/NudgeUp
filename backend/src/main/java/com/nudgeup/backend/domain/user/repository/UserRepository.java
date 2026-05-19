package com.nudgeup.backend.domain.user.repository;

import com.nudgeup.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// TODO: User 엔티티 DB 접근 인터페이스
// - findByEmail(): 로그인 시 이메일로 사용자 조회
// - existsByEmail(): 회원가입 시 이메일 중복 검사
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
