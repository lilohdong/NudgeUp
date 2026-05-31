package com.nudgeup.backend.domain.nudge.repository;

import com.nudgeup.backend.domain.nudge.entity.NudgeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NudgeLogRepository extends JpaRepository<NudgeLog, Long> {

    List<NudgeLog> findByTaskId(Long taskId);

    long countByTaskId(Long taskId);

    @Query("SELECT n FROM NudgeLog n WHERE n.taskId IN :taskIds ORDER BY n.sentAt DESC")
    List<NudgeLog> findByTaskIdIn(@Param("taskIds") List<Long> taskIds);

    @Modifying
    @Query("DELETE FROM NudgeLog n WHERE n.taskId IN :taskIds")
    void deleteByTaskIdIn(@Param("taskIds") List<Long> taskIds);
}
