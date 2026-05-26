package com.nudgeup.backend.domain.team.repository;

import com.nudgeup.backend.domain.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// TODO: Team 엔티티 DB 접근 인터페이스
// - findByInviteCode(): 초대 코드로 팀 조회 (팀 참가 시 사용)
public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);
}
