package com.nudgeup.backend.domain.task.entity;

// TODO: 작업 상태 Enum
// - PENDING: 대기 (생성 시 초기값)
// - IN_PROGRESS: 진행 중
// - COMPLETED: 완료 (이 상태에서는 독촉 불가)
public enum TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED
}
