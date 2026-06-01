package com.nudgeup.backend.domain.nudge.repository;

import com.nudgeup.backend.domain.nudge.entity.NudgeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NudgeLogRepository extends JpaRepository<NudgeLog, Long> {

    List<NudgeLog> findByTaskId(Long taskId);

    long countByTaskId(Long taskId);

    @Query("SELECT n FROM NudgeLog n WHERE n.taskId IN :taskIds ORDER BY n.sentAt DESC")
    List<NudgeLog> findByTaskIdIn(@Param("taskIds") List<Long> taskIds);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM NudgeLog n WHERE n.taskId IN :taskIds")
    void deleteByTaskIdIn(@Param("taskIds") List<Long> taskIds);

    @Query("SELECT n.taskId, COUNT(n) FROM NudgeLog n WHERE n.taskId IN :taskIds GROUP BY n.taskId")
    List<Object[]> countByTaskIdIn(@Param("taskIds") List<Long> taskIds);

    @Query("SELECT n FROM NudgeLog n WHERE n.taskId IN " +
           "(SELECT t.id FROM Task t WHERE t.assigneeId = :userId) " +
           "ORDER BY n.sentAt DESC")
    List<NudgeLog> findReceivedByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(n) FROM NudgeLog n WHERE n.taskId IN " +
           "(SELECT t.id FROM Task t WHERE t.assigneeId = :userId) " +
           "AND n.sentAt >= :since")
    long countReceivedByUserIdSince(@Param("userId") Long userId,
                                    @Param("since") LocalDateTime since);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM NudgeLog n WHERE n.sentById = :userId")
    void deleteBySentById(@Param("userId") Long userId);
}
