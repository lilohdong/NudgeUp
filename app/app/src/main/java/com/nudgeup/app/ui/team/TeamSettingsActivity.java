package com.nudgeup.app.ui.team;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.nudgeup.app.R;
import com.nudgeup.app.data.dto.response.MemberResponse;
import java.util.List;

public class TeamSettingsActivity extends AppCompatActivity {

    private TeamSettingsViewModel viewModel;
    private long teamId;
    private MemberNudgeAdapter memberNudgeAdapter;
    private TextView tvSelectedLeader;
    private long selectedLeaderId = -1L;
    private List<MemberResponse> memberList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_settings);

        teamId = getIntent().getLongExtra("teamId", -1);
        viewModel = new ViewModelProvider(this).get(TeamSettingsViewModel.class);

        View btnClose = findViewById(R.id.btn_close);
        Button btnSave = findViewById(R.id.btn_save);
        LinearLayout dropdownLeader = findViewById(R.id.dropdown_leader);
        tvSelectedLeader = findViewById(R.id.tv_selected_leader);
        RecyclerView rvMembers = findViewById(R.id.rv_members);

        rvMembers.setLayoutManager(new LinearLayoutManager(this));

        btnClose.setOnClickListener(v -> finish());

        dropdownLeader.setOnClickListener(v -> {
            if (memberList != null && !memberList.isEmpty()) {
                showLeaderDropdown(dropdownLeader);
            }
        });

        viewModel.getMembers().observe(this, members -> {
            if (members == null || members.isEmpty()) return;
            memberList = members;

            // 현재 팀장 초기 표시
            for (MemberResponse m : members) {
                if ("LEADER".equals(m.getRole())) {
                    selectedLeaderId = m.getUserId();
                    tvSelectedLeader.setText(m.getName() + " (현 팀장)");
                    viewModel.setNewLeader(m.getUserId());
                    break;
                }
            }

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

    private void showLeaderDropdown(View anchor) {
        // 팝업 컨테이너
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundResource(R.drawable.bg_dropdown);
        container.setPadding(0, dpToPx(4), 0, dpToPx(4));

        // PopupWindow 먼저 생성 (람다에서 참조하기 위해)
        PopupWindow popup = new PopupWindow(container,
                anchor.getWidth(),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);
        popup.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        popup.setElevation(16f);
        popup.setOutsideTouchable(true);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < memberList.size(); i++) {
            MemberResponse member = memberList.get(i);
            View itemView = inflater.inflate(R.layout.item_leader_dropdown, container, false);
            TextView tvName = itemView.findViewById(R.id.tv_leader_name);
            ImageView ivCheck = itemView.findViewById(R.id.iv_leader_check);

            boolean isCurrentLeader = "LEADER".equals(member.getRole());
            String label = member.getName() + (isCurrentLeader ? " (현 팀장)" : "");
            tvName.setText(label);

            boolean isSelected = member.getUserId() == selectedLeaderId;
            ivCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            tvName.setTextColor(isSelected ? 0xFFD4FF00 : 0xFFCCCCCC);

            final long memberId = member.getUserId();
            final String memberLabel = label;
            itemView.setOnClickListener(v -> {
                selectedLeaderId = memberId;
                viewModel.setNewLeader(memberId);
                tvSelectedLeader.setText(memberLabel);
                popup.dismiss();
            });

            container.addView(itemView);

            // 구분선 (마지막 항목 제외)
            if (i < memberList.size() - 1) {
                View divider = new View(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 1);
                lp.setMarginStart(dpToPx(16));
                lp.setMarginEnd(dpToPx(16));
                divider.setLayoutParams(lp);
                divider.setBackgroundColor(0xFF2A2A45);
                container.addView(divider);
            }
        }

        popup.showAsDropDown(anchor, 0, dpToPx(4));
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
