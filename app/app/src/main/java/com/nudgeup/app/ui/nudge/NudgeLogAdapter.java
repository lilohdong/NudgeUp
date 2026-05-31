package com.nudgeup.app.ui.nudge;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.nudgeup.app.R;
import com.nudgeup.app.data.dto.response.NudgeLogItemResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NudgeLogAdapter extends ListAdapter<NudgeLogItemResponse, NudgeLogAdapter.ViewHolder> {

    public NudgeLogAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nudge_log, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NudgeLogItemResponse item = getItem(position);

        String teamName = item.getTeamName() != null ? item.getTeamName() : "";
        String taskTitle = item.getTaskTitle() != null ? item.getTaskTitle() : "";
        holder.tvTeamTask.setText("[" + teamName + "]  " + taskTitle);

        String senderName = (item.getSentBy() != null && item.getSentBy().getName() != null)
                ? item.getSentBy().getName() : "누군가";
        holder.tvMessage.setText(senderName + "님이 독촉을 보냈습니다");

        holder.tvTime.setText(formatRelativeTime(item.getSentAt()));
    }

    private String formatRelativeTime(String sentAtStr) {
        if (sentAtStr == null) return "";
        try {
            String normalized = sentAtStr.replace("T", " ");
            if (normalized.length() > 19) normalized = normalized.substring(0, 19);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date sentAt = sdf.parse(normalized);
            if (sentAt == null) return "";
            long diffMs = new Date().getTime() - sentAt.getTime();
            long minutes = diffMs / 60000;
            if (minutes < 1)    return "방금 전";
            if (minutes < 60)   return minutes + "분 전";
            long hours = minutes / 60;
            if (hours < 24)     return hours + "시간 전";
            return (hours / 24) + "일 전";
        } catch (ParseException e) {
            return "";
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTeamTask, tvMessage, tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTeamTask = itemView.findViewById(R.id.tv_nudge_team_task);
            tvMessage  = itemView.findViewById(R.id.tv_nudge_message);
            tvTime     = itemView.findViewById(R.id.tv_nudge_time);
        }
    }

    private static final DiffUtil.ItemCallback<NudgeLogItemResponse> DIFF =
            new DiffUtil.ItemCallback<NudgeLogItemResponse>() {
                @Override
                public boolean areItemsTheSame(@NonNull NudgeLogItemResponse o,
                                               @NonNull NudgeLogItemResponse n) {
                    return o.getId() == n.getId();
                }
                @Override
                public boolean areContentsTheSame(@NonNull NudgeLogItemResponse o,
                                                  @NonNull NudgeLogItemResponse n) {
                    return o.getId() == n.getId();
                }
            };
}
