package com.nudgeup.app.ui.team;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.nudgeup.app.R;
import com.nudgeup.app.util.ToastUtils;
import com.nudgeup.app.data.dto.response.MemberResponse;
import java.util.List;

public class TeamSettingsActivity extends AppCompatActivity {

    private TeamSettingsViewModel viewModel;
    private long teamId;
    private MemberNudgeAdapter memberNudgeAdapter;
    private MemberNudgeAdapter memberReadonlyAdapter;
    private RecyclerView rvMembersReadonly;
    private TextView tvSelectedLeader;
    private TextView tvInviteCode;
    private long selectedLeaderId = -1L;
    private List<MemberResponse> memberList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_settings);

        teamId = getIntent().getLongExtra("teamId", -1);
        String myRole = getIntent().getStringExtra("myRole");
        boolean isLeader = "LEADER".equals(myRole);

        viewModel = new ViewModelProvider(this, new TeamSettingsViewModel.Factory()).get(TeamSettingsViewModel.class);

        View btnClose             = findViewById(R.id.btn_close);
        Button btnSave            = findViewById(R.id.btn_save);
        if (!isLeader) btnSave.setVisibility(android.view.View.GONE);
        LinearLayout dropdownLeader = findViewById(R.id.dropdown_leader);
        tvSelectedLeader          = findViewById(R.id.tv_selected_leader);
        tvInviteCode              = findViewById(R.id.tv_invite_code);
        ImageButton btnCopyInvite = findViewById(R.id.btn_copy_invite);
        RecyclerView rvMembers    = findViewById(R.id.rv_members);

        TextView tvTitle = findViewById(R.id.tv_title);
        if (tvTitle != null) tvTitle.setText(isLeader ? "팀 설정" : "팀 정보");

        View layoutLeaderSettings = findViewById(R.id.layout_leader_settings);
        if (layoutLeaderSettings != null) {
            layoutLeaderSettings.setVisibility(isLeader ? View.VISIBLE : View.GONE);
        }
        View layoutMemberReadonly = findViewById(R.id.layout_member_readonly);
        if (layoutMemberReadonly != null) {
            layoutMemberReadonly.setVisibility(isLeader ? View.GONE : View.VISIBLE);
        }

        rvMembers.setLayoutManager(new LinearLayoutManager(this));

        rvMembersReadonly = findViewById(R.id.rv_members_readonly);
        if (rvMembersReadonly != null) {
            rvMembersReadonly.setLayoutManager(new LinearLayoutManager(this));
        }

        btnClose.setOnClickListener(v -> finish());

        // 초대 코드 복사
        btnCopyInvite.setOnClickListener(v -> copyInviteCode());
        tvInviteCode.setOnClickListener(v -> copyInviteCode()); // 코드 텍스트 클릭도 복사

        dropdownLeader.setOnClickListener(v -> {
            if (memberList != null && !memberList.isEmpty()) {
                showLeaderDropdown(dropdownLeader);
            }
        });

        // 초대 코드 LiveData 관찰
        viewModel.getInviteCode().observe(this, code -> {
            if (code != null) tvInviteCode.setText(code);
        });

        // 멤버 목록 LiveData 관찰
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
            MemberNudgeAdapter.OnKickListener kickCb = isLeader ? (memberId, memberName) -> {
                android.view.View confirmView = getLayoutInflater()
                        .inflate(R.layout.dialog_delete_task, null);
                ((android.widget.TextView) confirmView.findViewById(R.id.tv_dialog_title))
                        .setText("팀원 추방");
                ((android.widget.TextView) confirmView.findViewById(R.id.tv_dialog_message))
                        .setText(memberName + "님을 팀에서 추방하시겠습니까?");
                ((android.widget.TextView) confirmView.findViewById(R.id.btn_dialog_confirm))
                        .setText("추방");
                android.app.AlertDialog d = new android.app.AlertDialog.Builder(TeamSettingsActivity.this)
                        .setView(confirmView).create();
                if (d.getWindow() != null) d.getWindow().setBackgroundDrawable(
                        new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                confirmView.findViewById(R.id.btn_dialog_cancel)
                        .setOnClickListener(btn -> d.dismiss());
                confirmView.findViewById(R.id.btn_dialog_confirm)
                        .setOnClickListener(btn -> {
                            d.dismiss();
                            viewModel.kickMember(teamId, memberId);
                        });
                d.show();
            } : null;

            if (memberNudgeAdapter == null) {
                memberNudgeAdapter = new MemberNudgeAdapter(members,
                        (userId, canNudge) -> viewModel.setNudgePermission(userId, canNudge),
                        kickCb);
                rvMembers.setAdapter(memberNudgeAdapter);
            } else {
                memberNudgeAdapter.setMembers(members);
            }

            // 팀원 전용 읽기 전용 목록
            if (!isLeader && rvMembersReadonly != null) {
                if (memberReadonlyAdapter == null) {
                    memberReadonlyAdapter = new MemberNudgeAdapter(members, null);
                    rvMembersReadonly.setAdapter(memberReadonlyAdapter);
                } else {
                    memberReadonlyAdapter.setMembers(members);
                }
            }
        });

        viewModel.getSaveResult().observe(this, success -> {
            if (success != null) {
                ToastUtils.show(this, success ? "저장 완료" : "저장 실패");
                if (success) finish();
            }
        });

        viewModel.getNetworkError().observe(this, msg -> {
            if (msg != null) ToastUtils.show(this, msg);
        });

        btnSave.setOnClickListener(v -> viewModel.saveSettings(teamId));

        Button btnLeaveTeam = findViewById(R.id.btn_leave_team);
        if (btnLeaveTeam != null) {
            btnLeaveTeam.setOnClickListener(v -> {
                android.view.View confirmView = getLayoutInflater().inflate(R.layout.dialog_delete_task, null);
                ((android.widget.TextView) confirmView.findViewById(R.id.tv_dialog_title))
                        .setText("팀 탈퇴");
                ((android.widget.TextView) confirmView.findViewById(R.id.tv_dialog_message))
                        .setText("정말 이 팀에서 탈퇴하시겠습니까?");
                ((android.widget.TextView) confirmView.findViewById(R.id.btn_dialog_confirm))
                        .setText("탈퇴");
                android.app.AlertDialog confirmDialog = new android.app.AlertDialog.Builder(this)
                        .setView(confirmView).create();
                if (confirmDialog.getWindow() != null) {
                    confirmDialog.getWindow().setBackgroundDrawable(
                            new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                }
                confirmView.findViewById(R.id.btn_dialog_cancel)
                        .setOnClickListener(btn -> confirmDialog.dismiss());
                confirmView.findViewById(R.id.btn_dialog_confirm)
                        .setOnClickListener(btn -> {
                            confirmDialog.dismiss();
                            viewModel.leaveTeam(teamId);
                        });
                confirmDialog.show();
            });
        }

        viewModel.getLeaveResult().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                setResult(RESULT_OK);
                finish();
            }
        });

        viewModel.getLeaveError().observe(this, msg -> {
            if (msg != null) com.nudgeup.app.util.ToastUtils.show(this, msg);
        });

        viewModel.loadMembers(teamId);
    }

    // ─────────────────────────────────────────
    //  초대 코드 클립보드 복사
    // ─────────────────────────────────────────

    private void copyInviteCode() {
        String code = tvInviteCode.getText().toString();
        if (code.isEmpty()) return;
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("초대 코드", code));
        ToastUtils.show(this, "초대 코드가 복사됐습니다");
    }

    // ─────────────────────────────────────────
    //  팀장 위임 커스텀 드롭다운
    // ─────────────────────────────────────────

    private void showLeaderDropdown(View anchor) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundResource(R.drawable.bg_dropdown);
        container.setPadding(0, dpToPx(4), 0, dpToPx(4));

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
            TextView tvName  = itemView.findViewById(R.id.tv_leader_name);
            ImageView ivCheck = itemView.findViewById(R.id.iv_leader_check);

            boolean isCurrentLeader = "LEADER".equals(member.getRole());
            String label = member.getName() + (isCurrentLeader ? " (현 팀장)" : "");
            tvName.setText(label);

            boolean isSelected = member.getUserId() == selectedLeaderId;
            ivCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            tvName.setTextColor(isSelected ? 0xFFD4FF00 : 0xFFCCCCCC);

            final long memberId    = member.getUserId();
            final String memberLabel = label;
            itemView.setOnClickListener(v -> {
                selectedLeaderId = memberId;
                viewModel.setNewLeader(memberId);
                tvSelectedLeader.setText(memberLabel);
                popup.dismiss();
            });

            container.addView(itemView);

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
