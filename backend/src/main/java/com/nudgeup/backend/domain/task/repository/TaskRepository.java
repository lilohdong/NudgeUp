package com.nudgeup.backend.domain.task.repository;

import com.nudgeup.backend.domain.task.entity.Task;
import com.nudgeup.backend.domain.task.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDateTime;
import java.util.List;

// TODO: Task 엔티티 DB 접근 인터페이스
// - findByTeamId(): 팀 전체 작업 조회
// - findByTeamIdAndStatus(): 상태 필터링 조회
// - findByTeamIdAndAssigneeId(): 내 작업 조회
// - findByTeamIdAndAssigneeIdAndStatus(): 내 작업 + 상태 필터
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByTeamId(Long teamId);

    List<Task> findByTeamIdAndStatus(Long teamId, TaskStatus status);

    List<Task> findByTeamIdAndStatusNot(Long teamId, TaskStatus status);

    List<Task> findByTeamIdAndAssigneeId(Long teamId, Long assigneeId);

    List<Task> findByTeamIdAndAssigneeIdAndStatus(Long teamId, Long assigneeId, TaskStatus status);

    List<Task> findByTeamIdAndAssigneeIdAndStatusNot(Long teamId, Long assigneeId, TaskStatus status);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Task t SET t.assigneeId = null WHERE t.assigneeId = :userId")
    void clearAssigneeByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Task t WHERE t.teamId = :teamId")
    void deleteAllByTeamId(@Param("teamId") Long teamId);

    @Query("SELECT t FROM Task t WHERE " +
           "(t.status = :completed AND t.completedAt < :cutoff) OR " +
           "(t.status = :cancelled AND t.cancelledAt < :cutoff)")
    List<Task> findExpiredTasks(@Param("completed") TaskStatus completed,
                                @Param("cancelled") TaskStatus cancelled,
                                @Param("cutoff") LocalDateTime cutoff);
}
