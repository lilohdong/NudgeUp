package com.nudgeup.backend.domain.task.repository;

import com.nudgeup.backend.domain.task.entity.Task;
import com.nudgeup.backend.domain.task.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// TODO: Task 엔티티 DB 접근 인터페이스
// - findByTeamId(): 팀 전체 작업 조회
// - findByTeamIdAndStatus(): 상태 필터링 조회
// - findByTeamIdAndAssigneeId(): 내 작업 조회
// - findByTeamIdAndAssigneeIdAndStatus(): 내 작업 + 상태 필터
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByTeamId(Long teamId);

    List<Task> findByTeamIdAndStatus(Long teamId, TaskStatus status);

    List<Task> findByTeamIdAndAssigneeId(Long teamId, Long assigneeId);

    List<Task> findByTeamIdAndAssigneeIdAndStatus(Long teamId, Long assigneeId, TaskStatus status);
}
