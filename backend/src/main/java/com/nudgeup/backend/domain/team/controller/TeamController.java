package com.nudgeup.backend.domain.team.controller;

import com.nudgeup.backend.domain.team.dto.request.CreateTeamRequest;
import com.nudgeup.backend.domain.team.dto.request.JoinTeamRequest;
import com.nudgeup.backend.domain.team.dto.request.UpdateTeamSettingsRequest;
import com.nudgeup.backend.domain.team.dto.response.*;
import com.nudgeup.backend.domain.team.service.TeamService;
import com.nudgeup.backend.global.response.ApiResponse;
import com.nudgeup.backend.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    public ResponseEntity<ApiResponse<TeamDetailResponse>> createTeam(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateTeamRequest request) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("팀이 생성되었습니다.", teamService.createTeam(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getMyTeams(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success("팀 목록 조회 성공", teamService.getMyTeams(userId)));
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<ApiResponse<TeamDetailResponse>> getTeamDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long teamId) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success("팀 조회 성공", teamService.getTeamDetail(userId, teamId)));
    }

    @GetMapping("/{teamId}/invite")
    public ResponseEntity<ApiResponse<InviteResponse>> getInviteCode(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long teamId) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success("초대 코드 조회 성공", teamService.getInviteCode(userId, teamId)));
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<TeamResponse>> joinTeam(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody JoinTeamRequest request) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success("팀에 참가했습니다.", teamService.joinTeam(userId, request)));
    }

    @PatchMapping("/{teamId}/settings")
    public ResponseEntity<ApiResponse<Void>> updateTeamSettings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long teamId,
            @RequestBody UpdateTeamSettingsRequest request) {
        Long userId = userDetails.getUser().getId();
        teamService.updateTeamSettings(userId, teamId, request);
        return ResponseEntity.ok(ApiResponse.success("팀 설정이 저장되었습니다.", null));
    }

    @DeleteMapping("/{teamId}/members/me")
    public ResponseEntity<ApiResponse<Void>> leaveTeam(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long teamId) {
        Long userId = userDetails.getUser().getId();
        teamService.leaveTeam(userId, teamId);
        return ResponseEntity.ok(ApiResponse.success("팀에서 탈퇴했습니다.", null));
    }
}
