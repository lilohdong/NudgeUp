package com.nudgeup.app.ui.main.adapter;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.nudgeup.app.R;
import com.nudgeup.app.data.dto.response.TaskResponse;
import java.util.Calendar;

public class TaskListAdapter extends ListAdapter<TaskResponse, TaskListAdapter.ViewHolder> {

    public interface OnNudgeClickListener {
        void onNudgeClick(long taskId);
    }

    public interface OnStatusChangeListener {
        void onStatusChange(long taskId, String newStatus);
    }

    private final OnNudgeClickListener nudgeListener;
    private final OnStatusChangeListener statusListener;
    private final boolean isMyTaskMode; // true = 내 할일 (수정 아이콘), false = 팀 할일 (종 아이콘)

    private static final int[] AVATAR_COLORS = {
        0xFF7B3FE4, // purple
        0xFF3498DB, // blue
        0xFFE74C3C, // red
        0xFF2ECC71, // green
        0xFFF39C12, // orange
        0xFF1ABC9C, // teal
    };

    public TaskListAdapter(OnNudgeClickListener nudgeListener, OnStatusChangeListener statusListener, boolean isMyTaskMode) {
        super(DIFF_CALLBACK);
        this.nudgeListener = nudgeListener;
        this.statusListener = statusListener;
        this.isMyTaskMode = isMyTaskMode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TaskResponse task = getItem(position);
        long dday = calcDday(task.getDueDate());
        boolean isUrgent = !"COMPLETED".equals(task.getStatus()) && dday <= 3;
        boolean isCompleted = "COMPLETED".equals(task.getStatus());

        // 상태 바 색상
        int statusColor;
        if (isCompleted) {
            statusColor = 0xFF2ECC71; // green
        } else if (isUrgent) {
            statusColor = 0xFFFF6B35; // orange
        } else if ("IN_PROGRESS".equals(task.getStatus())) {
            statusColor = 0xFF7B3FE4; // purple
        } else {
            statusColor = 0xFF4A4A6A; // gray
        }
        holder.vStatusBar.setBackgroundColor(statusColor);

        // 상태 배지 텍스트 + 배경
        String badgeText;
        int badgeTextColor;
        int badgeBgRes;
        if (isCompleted) {
            badgeText = "완료";
            badgeTextColor = 0xFF2ECC71;
            badgeBgRes = R.drawable.bg_badge_complete;
        } else if (isUrgent) {
            badgeText = "마감임박";
            badgeTextColor = 0xFFFF6B35;
            badgeBgRes = R.drawable.bg_badge_urgent;
        } else if ("IN_PROGRESS".equals(task.getStatus())) {
            badgeText = "진행중";
            badgeTextColor = 0xFF7B3FE4;
            badgeBgRes = R.drawable.bg_badge_progress;
        } else {
            badgeText = "대기";
            badgeTextColor = 0xFF8888AA;
            badgeBgRes = R.drawable.bg_badge_pending;
        }
        holder.tvStatusBadge.setText(badgeText);
        holder.tvStatusBadge.setTextColor(badgeTextColor);
        holder.tvStatusBadge.setBackgroundResource(badgeBgRes);

        // D-day 텍스트
        if (task.getDueDate() == null || task.getDueDate().isEmpty()) {
            holder.tvDday.setText("");
        } else if (isCompleted) {
            holder.tvDday.setText("완료됨");
            holder.tvDday.setTextColor(0xFF2ECC71);
        } else if (dday < 0) {
            holder.tvDday.setText("D+" + Math.abs(dday) + " 초과");
            holder.tvDday.setTextColor(0xFFFF6B35);
        } else if (dday == 0) {
            holder.tvDday.setText("D-Day");
            holder.tvDday.setTextColor(0xFFFF6B35);
        } else {
            holder.tvDday.setText("D-" + dday);
            holder.tvDday.setTextColor(dday <= 3 ? 0xFFFF6B35 : 0xFF8888AA);
        }

        // 작업명
        holder.tvTaskTitle.setText(task.getTitle());
        if (isCompleted) {
            holder.tvTaskTitle.setAlpha(0.5f);
        } else {
            holder.tvTaskTitle.setAlpha(1.0f);
        }

        // 담당자 아바타
        if (task.getAssignee() != null) {
            String name = task.getAssignee().getName();
            String initial = name != null && !name.isEmpty() ? String.valueOf(name.charAt(0)) : "?";
            holder.tvAssigneeAvatar.setText(initial);
            int avatarColor = AVATAR_COLORS[(int)(task.getAssignee().getId() % AVATAR_COLORS.length)];
            GradientDrawable avatarBg = new GradientDrawable();
            avatarBg.setShape(GradientDrawable.OVAL);
            avatarBg.setColor(avatarColor);
            holder.tvAssigneeAvatar.setBackground(avatarBg);
            holder.tvAssigneeName.setText(name);
        } else {
            holder.tvAssigneeAvatar.setText("?");
            holder.tvAssigneeName.setText("미지정");
        }

        // 액션 버튼
        if (isMyTaskMode) {
            // 내 할일: 항상 수정 아이콘으로 작업 이동
            holder.btnAction.setImageResource(R.drawable.ic_edit);
            holder.btnAction.setAlpha(1.0f);
            holder.btnAction.setOnClickListener(v -> nudgeListener.onNudgeClick(task.getId()));
        } else if (isCompleted) {
            holder.btnAction.setImageResource(R.drawable.ic_edit);
            holder.btnAction.setAlpha(0.4f);
            holder.btnAction.setOnClickListener(null);
        } else {
            holder.btnAction.setImageResource(R.drawable.ic_bell);
            holder.btnAction.setAlpha(1.0f);
            holder.btnAction.setOnClickListener(v -> nudgeListener.onNudgeClick(task.getId()));
        }

        // 상태 변경 (롱클릭으로 사이클)
        holder.itemView.setOnLongClickListener(v -> {
            if (!isCompleted) {
                statusListener.onStatusChange(task.getId(), nextStatus(task.getStatus()));
            }
            return true;
        });
    }

    private long calcDday(String dueDateStr) {
        if (dueDateStr == null) return 999L;
        try {
            String[] parts = dueDateStr.split("-");
            if (parts.length != 3) return 999L;
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1;
            int day = Integer.parseInt(parts[2]);

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            Calendar due = Calendar.getInstance();
            due.set(year, month, day, 0, 0, 0);
            due.set(Calendar.MILLISECOND, 0);

            long diffMs = due.getTimeInMillis() - today.getTimeInMillis();
            return diffMs / (24L * 60 * 60 * 1000);
        } catch (Exception e) {
            return 999L;
        }
    }

    private String nextStatus(String current) {
        if (current == null) return "IN_PROGRESS";
        switch (current) {
            case "PENDING":     return "IN_PROGRESS";
            case "IN_PROGRESS": return "COMPLETED";
            default:            return current;
        }
    }

    private static final DiffUtil.ItemCallback<TaskResponse> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<TaskResponse>() {
                @Override
                public boolean areItemsTheSame(@NonNull TaskResponse o, @NonNull TaskResponse n) {
                    return o.getId() == n.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull TaskResponse o, @NonNull TaskResponse n) {
                    return o.getStatus() != null && o.getStatus().equals(n.getStatus())
                            && o.getTitle() != null && o.getTitle().equals(n.getTitle());
                }
            };

    static class ViewHolder extends RecyclerView.ViewHolder {
        View vStatusBar;
        TextView tvStatusBadge, tvDday, tvTaskTitle, tvAssigneeAvatar, tvAssigneeName;
        ImageButton btnAction;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            vStatusBar = itemView.findViewById(R.id.v_status_bar);
            tvStatusBadge = itemView.findViewById(R.id.tv_status_badge);
            tvDday = itemView.findViewById(R.id.tv_dday);
            tvTaskTitle = itemView.findViewById(R.id.tv_task_title);
            tvAssigneeAvatar = itemView.findViewById(R.id.tv_assignee_avatar);
            tvAssigneeName = itemView.findViewById(R.id.tv_assignee_name);
            btnAction = itemView.findViewById(R.id.btn_action);
        }
    }
}
