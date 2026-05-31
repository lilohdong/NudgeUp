package com.nudgeup.app.ui.main.adapter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.nudgeup.app.R;
import com.nudgeup.app.data.dto.response.TeamResponse;
import java.util.List;

public class TeamTabAdapter {

    public interface OnTeamClickListener {
        void onTeamClick(TeamResponse team);
    }

    private static final int[] TEAM_COLORS = {
        0xFF7B3FE4, // purple
        0xFF3498DB, // blue
        0xFFE74C3C, // red
        0xFF2ECC71, // green
        0xFFF39C12, // orange
        0xFF1ABC9C, // teal
    };

    private final List<TeamResponse> teams;
    private final OnTeamClickListener listener;
    private long selectedTeamId = -1L;

    public TeamTabAdapter(List<TeamResponse> teams, OnTeamClickListener listener) {
        this.teams = teams;
        this.listener = listener;
    }

    public void setSelectedTeamId(long teamId) {
        this.selectedTeamId = teamId;
    }

    public void addTabsTo(LinearLayout container) {
        Context context = container.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        container.removeAllViews();

        for (int i = 0; i < teams.size(); i++) {
            TeamResponse team = teams.get(i);
            View tabView = inflater.inflate(R.layout.item_team_tab, container, false);

            View activeIndicator = tabView.findViewById(R.id.v_active_indicator);
            TextView tvInitial = tabView.findViewById(R.id.tv_team_initial);

            // 팀 초성
            String name = team.getName();
            String initial = name != null && !name.isEmpty() ? String.valueOf(name.charAt(0)) : "T";
            tvInitial.setText(initial);

            // 아바타 색상
            int color = parseColor(team.getColor(), TEAM_COLORS[i % TEAM_COLORS.length]);
            GradientDrawable avatarBg = new GradientDrawable();
            avatarBg.setShape(GradientDrawable.OVAL);
            avatarBg.setColor(color);
            tvInitial.setBackground(avatarBg);

            // 선택 표시 (라임 사이드바 위에서 어두운 오버레이 사용)
            if (team.getId() == selectedTeamId) {
                activeIndicator.setVisibility(View.VISIBLE);
                activeIndicator.setBackgroundColor(0xFF1A1A2E); // 어두운 인디케이터
                tabView.setBackgroundColor(0x25000000); // 미묘한 어두운 오버레이
            } else {
                activeIndicator.setVisibility(View.GONE);
                tabView.setBackgroundColor(0x00000000); // 투명
            }

            tabView.setOnClickListener(v -> {
                listener.onTeamClick(team);
            });

            container.addView(tabView);
        }
    }

    private int parseColor(String colorHex, int fallback) {
        if (colorHex != null && !colorHex.isEmpty()) {
            try {
                return android.graphics.Color.parseColor(colorHex);
            } catch (Exception ignored) {}
        }
        return fallback;
    }
}
