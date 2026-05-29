package com.nudgeup.app.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
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

        viewModel = new ViewModelProvider(this, new MainViewModel.Factory()).get(MainViewModel.class);

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
        Spinner spinnerTeamTasksSort = findViewById(R.id.spinner_team_tasks_sort);
        Spinner spinnerMyTasksSort = findViewById(R.id.spinner_my_tasks_sort);

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

        // 팀 설정 (getCurrentTeam이 갱신될 때마다 teamId를 읽어옴)
        btnTeamSettings.setOnClickListener(v -> {
            if (viewModel.getCurrentTeam().getValue() == null) return;
            Intent intent = new Intent(this, TeamSettingsActivity.class);
            intent.putExtra("teamId", viewModel.getCurrentTeam().getValue().getId());
            startActivity(intent);
        });

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

        // 정렬 Spinner 설정
        ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(
                this, R.array.sort_options, android.R.layout.simple_spinner_item);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerTeamTasksSort.setAdapter(sortAdapter);
        spinnerTeamTasksSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewModel.setTeamTasksSortMode(indexToSortMode(position));
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerMyTasksSort.setAdapter(sortAdapter);
        spinnerMyTasksSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewModel.setMyTasksSortMode(indexToSortMode(position));
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
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
            btnTeamSettings.setVisibility(View.VISIBLE);

            // 팀 탭 선택 표시 갱신
            if (teamTabAdapter != null) {
                teamTabAdapter.setSelectedTeamId(team.getId());
                teamTabAdapter.addTabsTo(sidebarTeamContainer);
            }
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

    private MainViewModel.SortMode indexToSortMode(int position) {
        switch (position) {
            case 1: return MainViewModel.SortMode.BY_PERSON;
            case 2: return MainViewModel.SortMode.BY_DEADLINE;
            default: return MainViewModel.SortMode.DEFAULT;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel.getCurrentTeam().getValue() != null) {
            viewModel.loadTasks(viewModel.getCurrentTeam().getValue().getId());
        }
    }
}
