package com.nudgeup.backend.domain.team.entity;

// TODO: 팀 내 역할 Enum
// - LEADER: 팀장 (팀 설정 변경, 독촉 권한 관리 가능)
// - MEMBER: 팀원 (독촉 권한은 canNudge 필드로 별도 관리)
public enum TeamRole {
    LEADER,
    MEMBER
}
