package com.nudgeup.backend.domain.nudge.controller;

import com.nudgeup.backend.domain.nudge.dto.response.NudgeLogResponse;
import com.nudgeup.backend.domain.nudge.dto.response.NudgeResponse;
import com.nudgeup.backend.domain.nudge.service.NudgeService;
import com.nudgeup.backend.global.response.ApiResponse;
import com.nudgeup.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// TODO: 독촉 알림 관련 엔드포인트
@RestController
@RequestMapping("/api/v1/teams/{teamId}")
@RequiredArgsConstructor
public class NudgeController {

    private final NudgeService nudgeService;

    // POST /api/v1/teams/{teamId}/tasks/{taskId}/nudge
    @PostMapping("/tasks/{taskId}/nudge")
    public ResponseEntity<ApiResponse<NudgeResponse>> sendNudge(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long teamId,
            @PathVariable Long taskId) {
        // TODO: 구현
        return null;
    }

    // GET /api/v1/teams/{teamId}/nudge-logs?taskId=
    @GetMapping("/nudge-logs")
    public ResponseEntity<ApiResponse<List<NudgeLogResponse>>> getNudgeLogs(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long teamId,
            @RequestParam(required = false) Long taskId) {
        // TODO: 구현
        return null;
    }
}
