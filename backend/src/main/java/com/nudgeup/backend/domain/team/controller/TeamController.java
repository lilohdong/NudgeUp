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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// TODO: 팀 관련 엔드포인트
@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    // POST /api/v1/teams
    @PostMapping
    public ResponseEntity<ApiResponse<TeamDetailResponse>> createTeam(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateTeamRequest request) {
        // TODO: 구현, 201 반환
        return null;
    }

    // GET /api/v1/teams
    @GetMapping
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getMyTeams(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // TODO: 구현
        return null;
    }

    // GET /api/v1/teams/{teamId}
    @GetMapping("/{teamId}")
    public ResponseEntity<ApiResponse<TeamDetailResponse>> getTeamDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long teamId) {
        // TODO: 구현
        return null;
    }

    // GET /api/v1/teams/{teamId}/invite
    @GetMapping("/{teamId}/invite")
    public ResponseEntity<ApiResponse<InviteResponse>> getInviteCode(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long teamId) {
        // TODO: 구현
        return null;
    }

    // POST /api/v1/teams/join
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<TeamResponse>> joinTeam(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody JoinTeamRequest request) {
        // TODO: 구현
        return null;
    }

    // PATCH /api/v1/teams/{teamId}/settings
    @PatchMapping("/{teamId}/settings")
    public ResponseEntity<ApiResponse<Void>> updateTeamSettings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long teamId,
            @RequestBody UpdateTeamSettingsRequest request) {
        // TODO: 구현
        return null;
    }

    // DELETE /api/v1/teams/{teamId}/members/me
    @DeleteMapping("/{teamId}/members/me")
    public ResponseEntity<ApiResponse<Void>> leaveTeam(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long teamId) {
        // TODO: 구현
        return null;
    }
}
