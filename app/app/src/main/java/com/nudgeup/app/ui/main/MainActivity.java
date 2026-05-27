package com.nudgeup.app.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.nudgeup.app.R;
import com.nudgeup.app.ui.main.adapter.MemberFilterAdapter;
import com.nudgeup.app.ui.main.adapter.TaskListAdapter;
import com.nudgeup.app.ui.main.adapter.TeamTabAdapter;
import com.nudgeup.app.ui.main.dialog.ProfileDialog;
import com.nudgeup.app.ui.task.CreateTaskActivity;
import com.nudgeup.app.ui.team.CreateTeamActivity;
import com.nudgeup.app.ui.team.TeamSettingsActivity;

public class MainActivity extends AppCompatActivity {

    private MainViewModel viewModel;
    private TaskListAdapter teamTaskAdapter;
    private TaskListAdapter myTaskAdapter;
    private MemberFilterAdapter memberFilterAdapter;
    private TeamTabAdapter teamTabAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // View 참조
        LinearLayout sidebarTeamContainer = findViewById(R.id.sidebar_team_container);
        TextView btnAddTeam = findViewById(R.id.btn_add_team);
        TextView tvTeamName = findViewById(R.id.tv_team_name);
        View btnTeamSettings = findViewById(R.id.btn_team_settings);
        TextView btnMy = findViewById(R.id.btn_my);
        RecyclerView rvMemberFilter = findViewById(R.id.rv_member_filter);
        RecyclerView rvTeamTasks = findViewById(R.id.rv_team_tasks);
        RecyclerView rvMyTasks = findViewById(R.id.rv_my_tasks);
        TextView tvTeamTaskCount = findViewById(R.id.tv_team_task_count);
        TextView tvMyTaskCount = findViewById(R.id.tv_my_task_count);
        TextView tvNudgeCount = findViewById(R.id.tv_nudge_count);
        View btnNewTask = findViewById(R.id.btn_new_task);

        // 팀원 필터 RecyclerView (가로)
        LinearLayoutManager filterLm = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvMemberFilter.setLayoutManager(filterLm);
        memberFilterAdapter = new MemberFilterAdapter(memberId -> viewModel.filterByMember(memberId));
        rvMemberFilter.setAdapter(memberFilterAdapter);

        // 팀원 전체 할일 RecyclerView (종 아이콘 = 독촉)
        rvTeamTasks.setLayoutManager(new LinearLayoutManager(this));
        teamTaskAdapter = new TaskListAdapter(
                taskId -> viewModel.sendNudge(taskId),
                (taskId, status) -> viewModel.updateTaskStatus(taskId, status),
                false
        );
        rvTeamTasks.setAdapter(teamTaskAdapter);

        // 내 할일 RecyclerView (수정 아이콘 = 작업 화면으로 이동)
        rvMyTasks.setLayoutManager(new LinearLayoutManager(this));
        myTaskAdapter = new TaskListAdapter(
                taskId -> {
                    Intent intent = new Intent(this, CreateTaskActivity.class);
                    intent.putExtra("teamId", viewModel.getCurrentTeam().getValue() != null
                            ? viewModel.getCurrentTeam().getValue().getId() : -1L);
                    startActivity(intent);
                },
                (taskId, status) -> viewModel.updateTaskStatus(taskId, status),
                true
        );
        rvMyTasks.setAdapter(myTaskAdapter);

        // 버튼 이벤트
        btnAddTeam.setOnClickListener(v ->
                startActivity(new Intent(this, CreateTeamActivity.class)));

        btnMy.setOnClickListener(v ->
                new ProfileDialog().show(getSupportFragmentManager(), "profile"));

        btnNewTask.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateTaskActivity.class);
            if (viewModel.getCurrentTeam().getValue() != null) {
                intent.putExtra("teamId", viewModel.getCurrentTeam().getValue().getId());
            }
            startActivity(intent);
        });

        // 관찰자 등록

        viewModel.getTeams().observe(this, teams -> {
            // 팀 탭 재생성
            long selectedId = viewModel.getCurrentTeam().getValue() != null
                    ? viewModel.getCurrentTeam().getValue().getId() : -1L;
            teamTabAdapter = new TeamTabAdapter(teams, team -> viewModel.selectTeam(team.getId()));
            teamTabAdapter.setSelectedTeamId(selectedId);
            teamTabAdapter.addTabsTo(sidebarTeamContainer);
        });

        viewModel.getCurrentTeam().observe(this, team -> {
            if (team == null) return;
            tvTeamName.setText(team.getName());

            // 팀 탭 선택 표시 갱신
            if (teamTabAdapter != null) {
                teamTabAdapter.setSelectedTeamId(team.getId());
                teamTabAdapter.addTabsTo(sidebarTeamContainer);
            }

            // 팀 설정 버튼 (항상 표시)
            btnTeamSettings.setVisibility(View.VISIBLE);
            btnTeamSettings.setOnClickListener(v -> {
                Intent intent = new Intent(this, TeamSettingsActivity.class);
                intent.putExtra("teamId", team.getId());
                startActivity(intent);
            });
        });

        viewModel.getMembers().observe(this, members ->
                memberFilterAdapter.setMembers(members));

        viewModel.getTeamTasks().observe(this, tasks -> {
            teamTaskAdapter.submitList(tasks);
            tvTeamTaskCount.setText(tasks.size() + "건");
        });

        viewModel.getMyTasks().observe(this, tasks -> {
            myTaskAdapter.submitList(tasks);
            tvMyTaskCount.setText(tasks.size() + "건");
        });

        viewModel.getNudgeCount().observe(this, count ->
                tvNudgeCount.setText(count + "회"));

        viewModel.getNudgeResult().observe(this, success -> {
            if (success != null) {
                Toast.makeText(this, success ? "독촉 알림을 보냈습니다." : "알림 전송에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.loadTeams();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel.getCurrentTeam().getValue() != null) {
            viewModel.loadTasks(viewModel.getCurrentTeam().getValue().getId());
        }
    }
}
