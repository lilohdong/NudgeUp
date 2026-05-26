package com.nudgeup.app.ui.team;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.nudgeup.app.R;
import com.nudgeup.app.data.dto.response.MemberResponse;
import java.util.ArrayList;
import java.util.List;

public class TeamSettingsActivity extends AppCompatActivity {

    private TeamSettingsViewModel viewModel;
    private long teamId;
    private MemberNudgeAdapter memberNudgeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_settings);

        teamId = getIntent().getLongExtra("teamId", -1);
        viewModel = new ViewModelProvider(this).get(TeamSettingsViewModel.class);

        View btnClose = findViewById(R.id.btn_close);
        Button btnSave = findViewById(R.id.btn_save);
        Spinner spinnerLeader = findViewById(R.id.spinner_new_leader);
        RecyclerView rvMembers = findViewById(R.id.rv_members);

        rvMembers.setLayoutManager(new LinearLayoutManager(this));

        btnClose.setOnClickListener(v -> finish());

        viewModel.getMembers().observe(this, members -> {
            if (members == null || members.isEmpty()) return;

            // 스피너 (팀장 위임)
            List<String> nameList = new ArrayList<>();
            for (MemberResponse m : members) {
                nameList.add(m.getName() + (m.getRole().equals("LEADER") ? " (현 팀장)" : ""));
            }
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, nameList);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerLeader.setAdapter(spinnerAdapter);

            // 현재 팀장 선택
            for (int i = 0; i < members.size(); i++) {
                if ("LEADER".equals(members.get(i).getRole())) {
                    spinnerLeader.setSelection(i);
                    break;
                }
            }

            spinnerLeader.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    viewModel.setNewLeader(members.get(position).getUserId());
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            // 독촉 권한 RecyclerView
            if (memberNudgeAdapter == null) {
                memberNudgeAdapter = new MemberNudgeAdapter(members,
                        (userId, canNudge) -> viewModel.setNudgePermission(userId, canNudge));
                rvMembers.setAdapter(memberNudgeAdapter);
            } else {
                memberNudgeAdapter.setMembers(members);
            }
        });

        viewModel.getSaveResult().observe(this, success -> {
            if (success != null) {
                Toast.makeText(this, success ? "저장 완료" : "저장 실패", Toast.LENGTH_SHORT).show();
                if (success) finish();
            }
        });

        btnSave.setOnClickListener(v -> viewModel.saveSettings(teamId));

        viewModel.loadMembers(teamId);
    }
}
