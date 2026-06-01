package com.nudgeup.app.ui.main.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.nudgeup.app.R;
import com.nudgeup.app.data.dto.response.TaskResponse;
import com.nudgeup.app.util.DateUtils;
import java.util.Objects;

public class TaskListAdapter extends ListAdapter<TaskResponse, TaskListAdapter.ViewHolder> {

    public interface OnNudgeClickListener {
        void onNudgeClick(long taskId);
    }

    public interface OnStatusChangeListener {
        void onStatusChange(long taskId, String newStatus);
    }

    public interface OnCancelClickListener {
        void onCancelClick(long taskId);
    }

    private final OnNudgeClickListener nudgeListener;
    private final OnStatusChangeListener statusListener;
    private final OnCancelClickListener cancelListener;
    private final boolean isMyTaskMode; // true = 내 할일 (수정 아이콘), false = 팀 할일 (종 아이콘)
    private final long currentUserId;
    private boolean isCurrentUserLeader = false;

    public void setCurrentUserLeader(boolean isLeader) {
        this.isCurrentUserLeader = isLeader;
        notifyDataSetChanged();
    }

    private static final int[] AVATAR_COLORS = {
        0xFF7B3FE4, // purple
        0xFF3498DB, // blue
        0xFFE74C3C, // red
        0xFF2ECC71, // green
        0xFFF39C12, // orange
        0xFF1ABC9C, // teal
    };

    // 상태 드롭다운 옵션 정의
    private static final String[] STATUS_KEYS   = {"PENDING",   "IN_PROGRESS", "COMPLETED"};
    private static final String[] STATUS_LABELS = {"대기",       "진행중",       "완료"};
    private static final int[]    STATUS_COLORS = {0xFF8888AA,  0xFF7B3FE4,    0xFF2ECC71};

    public TaskListAdapter(OnNudgeClickListener nudgeListener,
                           OnStatusChangeListener statusListener,
                           OnCancelClickListener cancelListener,
                           boolean isMyTaskMode,
                           long currentUserId) {
        super(DIFF_CALLBACK);
        this.nudgeListener  = nudgeListener;
        this.statusListener = statusListener;
        this.cancelListener = cancelListener;
        this.isMyTaskMode   = isMyTaskMode;
        this.currentUserId  = currentUserId;
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
        long dday        = calcDday(task.getDueDate());
        boolean isCancelled = "CANCELLED".equals(task.getStatus());
        boolean isCompleted = "COMPLETED".equals(task.getStatus());
        boolean isUrgent    = !isCompleted && !isCancelled && dday >= 0 && dday <= 3;
        boolean isOverdue   = !isCompleted && !isCancelled && dday < 0;

        // ── 상태 바 색상 ──
        int statusColor;
        if (isCompleted)                                 statusColor = 0xFF2ECC71;
        else if (isCancelled)                            statusColor = 0xFF555577;
        else if (isOverdue)                              statusColor = 0xFFFF6B35;
        else if (isUrgent)                               statusColor = 0xFFFF6B35;
        else if ("IN_PROGRESS".equals(task.getStatus())) statusColor = 0xFF7B3FE4;
        else                                             statusColor = 0xFF4A4A6A;
        holder.vStatusBar.setBackgroundColor(statusColor);

        // ── 상태 배지 ──
        applyStatusBadge(holder.tvStatusBadge, task);

        // 상태 배지 — UI 표시 전용, 클릭 비활성화
        holder.tvStatusBadge.setOnClickListener(null);
        holder.tvStatusBadge.setClickable(false);

        // ── D-day 텍스트 ──
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

        // ── 작업명 ──
        holder.tvTaskTitle.setText(task.getTitle());
        holder.tvTaskTitle.setAlpha((isCompleted || isCancelled) ? 0.5f : 1.0f);

        // ── 담당자 아바타 ──
        if (task.getAssignee() != null) {
            String name = task.getAssignee().getName();
            String initial = (name != null && !name.isEmpty())
                    ? String.valueOf(name.charAt(0)) : "?";
            holder.tvAssigneeAvatar.setText(initial);
            int fallback = AVATAR_COLORS[(int)(task.getAssignee().getId() % AVATAR_COLORS.length)];
            int avatarColor = parseColor(task.getAssignee().getColor(), fallback);
            holder.avatarBg.setColor(avatarColor);
            holder.tvAssigneeName.setText(name);
        } else {
            holder.tvAssigneeAvatar.setText("?");
            holder.avatarBg.setColor(0xFF4A4A6A);
            holder.tvAssigneeName.setText("미지정");
        }

        // ── 액션 버튼 ──
        boolean isMyOwnTask = task.getAssignee() != null && task.getAssignee().getId() == currentUserId;
        if (isCancelled) {
            holder.btnAction.setAlpha(0.0f);
            holder.btnAction.setOnClickListener(null);
        } else if (isMyTaskMode) {
            // 내 할일 탭: 완료 작업은 버튼 비활성, 나머지는 상태 변경 가능
            if (isCompleted) {
                holder.btnAction.setImageResource(R.drawable.ic_bell);
                holder.btnAction.setAlpha(0.4f);
                holder.btnAction.setOnClickListener(null);
            } else {
                holder.btnAction.setImageResource(R.drawable.ic_edit);
                holder.btnAction.setAlpha(1.0f);
                holder.btnAction.setOnClickListener(v -> showStatusDropdown(v, task.getId(), task.getStatus()));
            }
        } else {
            // 팀 전체 할일 탭: 담당자 또는 팀장 → 상태 변경(pen), 나머지 → 독촉(bell)
            boolean canChangeStatus = (isMyOwnTask || isCurrentUserLeader) && !isCompleted;
            if (canChangeStatus) {
                holder.btnAction.setImageResource(R.drawable.ic_edit);
                holder.btnAction.setAlpha(1.0f);
                holder.btnAction.setOnClickListener(v -> showStatusDropdown(v, task.getId(), task.getStatus()));
            } else if (isCompleted) {
                holder.btnAction.setImageResource(R.drawable.ic_bell);
                holder.btnAction.setAlpha(0.4f);
                holder.btnAction.setOnClickListener(null);
            } else {
                holder.btnAction.setImageResource(R.drawable.ic_bell);
                holder.btnAction.setAlpha(1.0f);
                holder.btnAction.setOnClickListener(v -> nudgeListener.onNudgeClick(task.getId()));
            }
        }

        // ── 카드 클릭 → 상세 다이얼로그 ──
        holder.itemView.setOnClickListener(v -> showDetailDialog(v, task));

        // ── 삭제 버튼 ──
        if (isCancelled) {
            holder.btnCancel.setVisibility(View.GONE);
        } else {
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setAlpha(0.6f);
            holder.btnCancel.setOnClickListener(v -> showDeleteDialog(v, task));
        }
    }

    // ─────────────────────────────────────────
    //  작업 상세 다이얼로그
    // ─────────────────────────────────────────

    private void showDetailDialog(View anchor, TaskResponse task) {
        Context context = anchor.getContext();
        android.view.View dialogView = android.view.LayoutInflater.from(context)
                .inflate(R.layout.dialog_task_detail, null);

        android.widget.TextView tvTitle       = dialogView.findViewById(R.id.tv_detail_title);
        android.widget.TextView tvStatus      = dialogView.findViewById(R.id.tv_detail_status);
        android.widget.TextView tvDueDate     = dialogView.findViewById(R.id.tv_detail_due_date);
        android.widget.TextView tvAssignee    = dialogView.findViewById(R.id.tv_detail_assignee);
        android.widget.LinearLayout layoutDesc = dialogView.findViewById(R.id.layout_detail_description);
        android.widget.TextView tvDesc        = dialogView.findViewById(R.id.tv_detail_description);
        android.widget.TextView btnClose      = dialogView.findViewById(R.id.btn_detail_close);

        tvTitle.setText(task.getTitle());

        // 상태 배지
        applyStatusBadge(tvStatus, task);

        // 마감일
        boolean isCancelled = "CANCELLED".equals(task.getStatus());
        boolean isCompleted = "COMPLETED".equals(task.getStatus());
        if (task.getDueDate() == null || task.getDueDate().isEmpty()) {
            tvDueDate.setText("없음");
            tvDueDate.setTextColor(0xFF8888AA);
        } else {
            long dday = calcDday(task.getDueDate());
            if (isCompleted) {
                tvDueDate.setText(task.getDueDate() + "  (완료됨)");
                tvDueDate.setTextColor(0xFF2ECC71);
            } else if (dday < 0) {
                tvDueDate.setText(task.getDueDate() + "  (D+" + Math.abs(dday) + " 초과)");
                tvDueDate.setTextColor(0xFFFF6B35);
            } else if (dday == 0) {
                tvDueDate.setText(task.getDueDate() + "  (D-Day)");
                tvDueDate.setTextColor(0xFFFF6B35);
            } else {
                tvDueDate.setText(task.getDueDate() + "  (D-" + dday + ")");
                tvDueDate.setTextColor(dday <= 3 ? 0xFFFF6B35 : 0xFFCCCCDD);
            }
        }

        // 담당자
        tvAssignee.setText(task.getAssignee() != null ? task.getAssignee().getName() : "미지정");

        // 상세내용
        String desc = task.getDescription();
        if (desc != null && !desc.trim().isEmpty()) {
            layoutDesc.setVisibility(android.view.View.VISIBLE);
            tvDesc.setText(desc.trim());
        } else {
            layoutDesc.setVisibility(android.view.View.GONE);
        }

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // ─────────────────────────────────────────
    //  삭제 확인 다이얼로그
    // ─────────────────────────────────────────

    private void showDeleteDialog(View anchor, TaskResponse task) {
        Context context = anchor.getContext();
        android.view.View dialogView = android.view.LayoutInflater.from(context)
                .inflate(R.layout.dialog_delete_task, null);

        android.widget.TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        android.widget.TextView tvMessage = dialogView.findViewById(R.id.tv_dialog_message);
        android.widget.TextView btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        android.widget.TextView btnConfirm = dialogView.findViewById(R.id.btn_dialog_confirm);

        tvTitle.setText("작업 삭제");
        tvMessage.setText("'" + task.getTitle() + "' 작업을 삭제하시겠습니까?\n삭제된 작업은 3일 후 자동으로 제거됩니다.");

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            cancelListener.onCancelClick(task.getId());
            dialog.dismiss();
        });

        dialog.show();
    }

    // ─────────────────────────────────────────
    //  상태 변경 드롭다운 (PopupWindow)
    // ─────────────────────────────────────────

    private void showStatusDropdown(View anchor, long taskId, String currentStatus) {
        Context context = anchor.getContext();

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundResource(R.drawable.bg_dropdown);
        container.setPadding(0, dpToPx(context, 4), 0, dpToPx(context, 4));

        PopupWindow popup = new PopupWindow(
                container,
                dpToPx(context, 130),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setElevation(16f);
        popup.setOutsideTouchable(true);

        LayoutInflater inflater = LayoutInflater.from(context);
        for (int i = 0; i < STATUS_KEYS.length; i++) {
            final String key = STATUS_KEYS[i];
            String label     = STATUS_LABELS[i];
            int color        = STATUS_COLORS[i];

            View itemView  = inflater.inflate(R.layout.item_leader_dropdown, container, false);
            TextView tvName = itemView.findViewById(R.id.tv_leader_name);
            ImageView ivCheck = itemView.findViewById(R.id.iv_leader_check);

            tvName.setText(label);
            tvName.setTextColor(color);
            ivCheck.setVisibility(key.equals(currentStatus) ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> {
                statusListener.onStatusChange(taskId, key);
                popup.dismiss();
            });

            container.addView(itemView);

            // 구분선 (마지막 항목 제외)
            if (i < STATUS_KEYS.length - 1) {
                View divider = new View(context);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 1);
                lp.setMarginStart(dpToPx(context, 16));
                lp.setMarginEnd(dpToPx(context, 16));
                divider.setLayoutParams(lp);
                divider.setBackgroundColor(0xFF2A2A45);
                container.addView(divider);
            }
        }

        popup.showAsDropDown(anchor, 0, dpToPx(context, 4));
    }

    // ─────────────────────────────────────────
    //  상태 배지 렌더링 공통 헬퍼
    // ─────────────────────────────────────────

    /**
     * task 의 상태/기한에 따라 배지 TextView 의 텍스트·색상·배경을 일괄 적용.
     * onBindViewHolder 와 showDetailDialog 양쪽에서 공유.
     */
    private void applyStatusBadge(TextView badge, TaskResponse task) {
        long dday = calcDday(task.getDueDate());
        boolean isCancelled = "CANCELLED".equals(task.getStatus());
        boolean isCompleted = "COMPLETED".equals(task.getStatus());
        boolean isOverdue   = !isCompleted && !isCancelled && dday < 0;
        boolean isUrgent    = !isCompleted && !isCancelled && dday >= 0 && dday <= 3;

        String badgeText;
        int badgeTextColor;
        int badgeBgRes;
        if (isCancelled) {
            badgeText      = "중단";
            badgeTextColor = 0xFF888899;
            badgeBgRes     = R.drawable.bg_badge_pending;
        } else if (isCompleted) {
            badgeText      = "완료";
            badgeTextColor = 0xFF2ECC71;
            badgeBgRes     = R.drawable.bg_badge_complete;
        } else if (isOverdue) {
            badgeText      = "기한초과";
            badgeTextColor = 0xFFFF6B35;
            badgeBgRes     = R.drawable.bg_badge_urgent;
        } else if (isUrgent) {
            badgeText      = "마감임박";
            badgeTextColor = 0xFFFF6B35;
            badgeBgRes     = R.drawable.bg_badge_urgent;
        } else if ("IN_PROGRESS".equals(task.getStatus())) {
            badgeText      = "진행중";
            badgeTextColor = 0xFF7B3FE4;
            badgeBgRes     = R.drawable.bg_badge_progress;
        } else {
            badgeText      = "대기";
            badgeTextColor = 0xFF8888AA;
            badgeBgRes     = R.drawable.bg_badge_pending;
        }
        badge.setText(badgeText);
        badge.setTextColor(badgeTextColor);
        badge.setBackgroundResource(badgeBgRes);
    }

    // ─────────────────────────────────────────
    //  D-day 계산 — DateUtils 공통 유틸 위임
    // ─────────────────────────────────────────

    private long calcDday(String dueDateStr) {
        return DateUtils.calcDday(dueDateStr);
    }

    private int dpToPx(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    private int parseColor(String colorHex, int fallback) {
        if (colorHex != null && !colorHex.isEmpty()) {
            try {
                return android.graphics.Color.parseColor(colorHex);
            } catch (Exception ignored) {}
        }
        return fallback;
    }

    // ─────────────────────────────────────────
    //  DiffUtil
    // ─────────────────────────────────────────

    private static final DiffUtil.ItemCallback<TaskResponse> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<TaskResponse>() {
                @Override
                public boolean areItemsTheSame(@NonNull TaskResponse o, @NonNull TaskResponse n) {
                    return o.getId() == n.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull TaskResponse o, @NonNull TaskResponse n) {
                    boolean statusSame  = Objects.equals(o.getStatus(), n.getStatus());
                    boolean titleSame   = Objects.equals(o.getTitle(), n.getTitle());
                    boolean dueDateSame = Objects.equals(o.getDueDate(), n.getDueDate());
                    long oldAssigneeId = o.getAssignee() != null ? o.getAssignee().getId() : -1L;
                    long newAssigneeId = n.getAssignee() != null ? n.getAssignee().getId() : -1L;
                    return statusSame && titleSame && dueDateSame && (oldAssigneeId == newAssigneeId);
                }
            };

    // ─────────────────────────────────────────
    //  ViewHolder
    // ─────────────────────────────────────────

    static class ViewHolder extends RecyclerView.ViewHolder {
        View vStatusBar;
        TextView tvStatusBadge, tvDday, tvTaskTitle, tvAssigneeAvatar, tvAssigneeName;
        ImageButton btnAction;
        ImageButton btnCancel;
        final GradientDrawable avatarBg;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            vStatusBar        = itemView.findViewById(R.id.v_status_bar);
            tvStatusBadge     = itemView.findViewById(R.id.tv_status_badge);
            tvDday            = itemView.findViewById(R.id.tv_dday);
            tvTaskTitle       = itemView.findViewById(R.id.tv_task_title);
            tvAssigneeAvatar  = itemView.findViewById(R.id.tv_assignee_avatar);
            tvAssigneeName    = itemView.findViewById(R.id.tv_assignee_name);
            btnAction         = itemView.findViewById(R.id.btn_action);
            btnCancel         = itemView.findViewById(R.id.btn_cancel_task);
            avatarBg = new GradientDrawable();
            avatarBg.setShape(GradientDrawable.OVAL);
            tvAssigneeAvatar.setBackground(avatarBg);
        }
    }
}
