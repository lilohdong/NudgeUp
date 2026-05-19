package com.nudgeup.backend.domain.task.controller;

import com.nudgeup.backend.domain.task.dto.request.CreateTaskRequest;
import com.nudgeup.backend.domain.task.dto.request.UpdateTaskStatusRequest;
import com.nudgeup.backend.domain.task.dto.response.TaskResponse;
import com.nudgeup.backend.domain.task.entity.TaskStatus;
import com.nudgeup.backend.domain.task.service.TaskService;
import com.nudgeup.backend.global.response.ApiResponse;
import com.nudgeup.backend.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// TODO: 작업 관련 엔드포인트
@RestController
@RequestMapping("/api/v1/teams/{teamId}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    // POST /api/v1/teams/{teamId}/tasks
    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long teamId,
            @Valid @RequestBody CreateTaskRequest request) {
        // TODO: 구현, 201 반환
        return null;
    }

    // GET /api/v1/teams/{teamId}/tasks?status=
    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTeamTasks(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long teamId,
            @RequestParam(required = false) TaskStatus status) {
        // TODO: 구현
        return null;
    }

    // GET /api/v1/teams/{teamId}/tasks/my?status=
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getMyTasks(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long teamId,
            @RequestParam(required = false) TaskStatus status) {
        // TODO: 구현
        return null;
    }

    // PATCH /api/v1/teams/{teamId}/tasks/{taskId}/status
    @PatchMapping("/{taskId}/status")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTaskStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long teamId,
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskStatusRequest request) {
        // TODO: 구현
        return null;
    }
}
