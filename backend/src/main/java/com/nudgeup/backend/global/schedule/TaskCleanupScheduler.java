package com.nudgeup.backend.global.schedule;

import com.nudgeup.backend.domain.nudge.repository.NudgeLogRepository;
import com.nudgeup.backend.domain.task.entity.Task;
import com.nudgeup.backend.domain.task.entity.TaskStatus;
import com.nudgeup.backend.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskCleanupScheduler {

    private final TaskRepository taskRepository;
    private final NudgeLogRepository nudgeLogRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteExpiredTasks() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(3);
        List<Task> expiredTasks = taskRepository.findExpiredTasks(
                TaskStatus.COMPLETED, TaskStatus.CANCELLED, cutoff);

        if (expiredTasks.isEmpty()) {
            return;
        }

        List<Long> taskIds = expiredTasks.stream()
                .map(Task::getId)
                .collect(Collectors.toList());

        nudgeLogRepository.deleteByTaskIdIn(taskIds);
        taskRepository.deleteAllById(taskIds);

        log.info("만료된 작업 {}건 자동 삭제 완료", taskIds.size());
    }
}
