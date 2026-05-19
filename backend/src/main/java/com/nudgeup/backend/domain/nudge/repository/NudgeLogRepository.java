package com.nudgeup.backend.domain.nudge.repository;

import com.nudgeup.backend.domain.nudge.entity.NudgeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// TODO: NudgeLog 엔티티 DB 접근 인터페이스
// - findByTaskId(): 특정 작업의 독촉 로그 조회
// - countByTaskId(): 작업별 독촉 횟수 집계 (TaskResponse.nudgeCount에 사용)
public interface NudgeLogRepository extends JpaRepository<NudgeLog, Long> {

    List<NudgeLog> findByTaskId(Long taskId);

    long countByTaskId(Long taskId);

    // TODO: 팀 전체 독촉 로그 조회를 위해
    // @Query("SELECT n FROM NudgeLog n WHERE n.taskId IN :taskIds ORDER BY n.sentAt DESC")
    // List<NudgeLog> findByTaskIdIn(List<Long> taskIds);
}
