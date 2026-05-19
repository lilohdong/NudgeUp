package com.nudgeup.backend.domain.nudge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// TODO: nudge_logs 테이블과 매핑되는 JPA 엔티티
// DB 컬럼: id, task_id, sent_by_id, sent_at
// - 독촉 발송 이력을 누적 저장 (삭제/수정 없음, 순수 로그 테이블)
// - sentAt: @CreationTimestamp로 자동 기록
@Entity
@Table(name = "nudge_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NudgeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "sent_by_id", nullable = false)
    private Long sentById;

    // TODO: @CreationTimestamp LocalDateTime sentAt 추가

    @Builder
    public NudgeLog(Long taskId, Long sentById) {
        // TODO: 구현
    }
}
