package com.nudgeup.backend.domain.task.entity;

import com.nudgeup.backend.global.exception.CustomException;
import com.nudgeup.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;

class TaskTest {

    private Task makeTask(TaskStatus status) {
        Task task = Task.builder()
                .teamId(1L)
                .title("테스트 작업")
                .createdById(1L)
                .assigneeId(1L)
                .dueDate(LocalDate.now().plusDays(7))
                .build();
        if (status == TaskStatus.IN_PROGRESS) {
            task.changeStatus(TaskStatus.IN_PROGRESS);
        } else if (status == TaskStatus.COMPLETED) {
            task.changeStatus(TaskStatus.IN_PROGRESS);
            task.changeStatus(TaskStatus.COMPLETED);
        }
        return task;
    }

    @Test
    void 완료된_작업을_진행중으로_되돌릴_수_있다() {
        Task task = makeTask(TaskStatus.COMPLETED);
        assertThatCode(() -> task.changeStatus(TaskStatus.IN_PROGRESS))
                .doesNotThrowAnyException();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void 완료된_작업을_대기로_되돌리면_예외() {
        Task task = makeTask(TaskStatus.COMPLETED);
        assertThatThrownBy(() -> task.changeStatus(TaskStatus.PENDING))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TASK_ALREADY_COMPLETED);
    }

    @Test
    void 진행중에서_완료로_변경_가능() {
        Task task = makeTask(TaskStatus.IN_PROGRESS);
        assertThatCode(() -> task.changeStatus(TaskStatus.COMPLETED))
                .doesNotThrowAnyException();
    }

    @Test
    void 완료에서_진행중으로_복구_시_completedAt이_초기화된다() {
        Task task = makeTask(TaskStatus.COMPLETED);
        assertThat(task.getCompletedAt()).isNotNull();
        task.changeStatus(TaskStatus.IN_PROGRESS);
        assertThat(task.getCompletedAt()).isNull();
    }

    @Test
    void 최초_진행중_전환_시만_startedAt이_설정된다() {
        // PENDING 상태 작업: startedAt이 null
        Task task = makeTask(TaskStatus.PENDING);
        assertThat(task.getStartedAt()).isNull();

        // 최초 IN_PROGRESS 전환: startedAt 설정됨
        task.changeStatus(TaskStatus.IN_PROGRESS);
        LocalDateTime firstStartedAt = task.getStartedAt();
        assertThat(firstStartedAt).isNotNull();

        // COMPLETED로 전환 후 IN_PROGRESS로 복구
        task.changeStatus(TaskStatus.COMPLETED);
        task.changeStatus(TaskStatus.IN_PROGRESS);

        // startedAt이 최초 값 그대로 보존되어야 함
        assertThat(task.getStartedAt()).isEqualTo(firstStartedAt);
    }
}
