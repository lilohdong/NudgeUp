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

@RestController
@RequestMapping("/api/v1/teams/{teamId}")
@RequiredArgsConstructor
public class NudgeController {

    private final NudgeService nudgeService;

    @PostMapping("/tasks/{taskId}/nudge")
    public ResponseEntity<ApiResponse<NudgeResponse>> sendNudge(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long teamId,
            @PathVariable Long taskId) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success("독촉 알림을 발송했습니다.", nudgeService.sendNudge(userId, teamId, taskId)));
    }

    @GetMapping("/nudge-logs")
    public ResponseEntity<ApiResponse<List<NudgeLogResponse>>> getNudgeLogs(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long teamId,
            @RequestParam(required = false) Long taskId) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success("독촉 로그 조회 성공", nudgeService.getNudgeLogs(userId, teamId, taskId)));
    }
}
