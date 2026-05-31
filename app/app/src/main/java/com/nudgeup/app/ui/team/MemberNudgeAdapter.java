package com.nudgeup.app.ui.team;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.nudgeup.app.R;
import com.nudgeup.app.data.dto.response.MemberResponse;
import java.util.List;

public class MemberNudgeAdapter extends RecyclerView.Adapter<MemberNudgeAdapter.ViewHolder> {

    public interface OnNudgePermissionChangeListener {
        void onChange(long userId, boolean canNudge);
    }

    public interface OnKickListener {
        void onKick(long memberId, String memberName);
    }

    private static final int[] AVATAR_COLORS = {
        0xFF7B3FE4, // purple
        0xFF3498DB, // blue
        0xFFE74C3C, // red
        0xFF2ECC71, // green
        0xFFF39C12, // orange
        0xFF1ABC9C, // teal
    };

    private List<MemberResponse> members;
    private final OnNudgePermissionChangeListener listener;
    private final OnKickListener kickListener;

    public MemberNudgeAdapter(List<MemberResponse> members, OnNudgePermissionChangeListener listener) {
        this(members, listener, null);
    }

    public MemberNudgeAdapter(List<MemberResponse> members, OnNudgePermissionChangeListener listener,
                               OnKickListener kickListener) {
        this.members = members;
        this.listener = listener;
        this.kickListener = kickListener;
    }

    public void setMembers(List<MemberResponse> members) {
        this.members = members;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member_nudge, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MemberResponse member = members.get(position);

        // 아바타 초성 + 색상
        String name = member.getName();
        String initial = name != null && !name.isEmpty() ? String.valueOf(name.charAt(0)) : "?";
        holder.tvAvatar.setText(initial);

        int color = AVATAR_COLORS[(int)(member.getUserId() % AVATAR_COLORS.length)];
        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        avatarBg.setColor(color);
        holder.tvAvatar.setBackground(avatarBg);

        holder.tvName.setText(name);

        // 역할 라벨
        boolean isLeader = "LEADER".equals(member.getRole());
        holder.tvRole.setText(isLeader ? "팀장" : "팀원");
        holder.tvRole.setTextColor(isLeader ? 0xFFD4FF00 : 0xFF8888AA);

        // 독촉 권한 토글 (팀장은 항상 ON, 비활성화)
        holder.swCanNudge.setOnCheckedChangeListener(null);
        holder.swCanNudge.setChecked(isLeader || member.isCanNudge());
        if (isLeader) {
            holder.swCanNudge.setEnabled(false);
        } else {
            holder.swCanNudge.setEnabled(true);
            holder.swCanNudge.setOnCheckedChangeListener((btn, checked) ->
                    listener.onChange(member.getUserId(), checked));
        }

        // 추방 버튼 (팀장 모드에서 팀원에게만 표시)
        if (kickListener != null && !isLeader) {
            holder.btnKick.setVisibility(View.VISIBLE);
            holder.btnKick.setOnClickListener(v ->
                    kickListener.onKick(member.getUserId(), member.getName()));
        } else {
            holder.btnKick.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return members == null ? 0 : members.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvRole;
        SwitchCompat swCanNudge;
        ImageButton btnKick;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_member_avatar);
            tvName = itemView.findViewById(R.id.tv_member_name);
            tvRole = itemView.findViewById(R.id.tv_member_role);
            swCanNudge = itemView.findViewById(R.id.sw_can_nudge);
            btnKick = itemView.findViewById(R.id.btn_kick);
        }
    }
}
