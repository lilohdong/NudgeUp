package com.nudgeup.backend.domain.team.repository;

import com.nudgeup.backend.domain.team.entity.TeamMember;
import com.nudgeup.backend.domain.team.entity.TeamRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// TODO: TeamMember 엔티티 DB 접근 인터페이스
// - findByTeamId(): 팀의 전체 멤버 목록 조회
// - findByTeamIdAndUserId(): 특정 팀에서 특정 유저의 멤버십 조회 (권한 검증 시 사용)
// - existsByTeamIdAndUserId(): 팀 중복 참가 검사
// - findAllByUserIdIn(): 유저의 모든 팀 멤버십 조회 (내 팀 목록 조회 시 사용)
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    List<TeamMember> findByTeamId(Long teamId);

    Optional<TeamMember> findByTeamIdAndUserId(Long teamId, Long userId);

    boolean existsByTeamIdAndUserId(Long teamId, Long userId);

    List<TeamMember> findAllByUserId(Long userId);

    long countByTeamId(Long teamId);
}
