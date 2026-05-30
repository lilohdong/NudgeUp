package com.nudgeup.app.ui.main.adapter;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.nudgeup.app.R;
import com.nudgeup.app.data.dto.response.MemberResponse;
import java.util.ArrayList;
import java.util.List;

public class MemberFilterAdapter extends RecyclerView.Adapter<MemberFilterAdapter.ViewHolder> {

    public interface OnMemberFilterListener {
        void onFilter(long memberId); // -1 = 전체
    }

    private static final int[] AVATAR_COLORS = {
        0xFF5B6B7A, // 채도 낮은 슬레이트 (전체 버튼)
        0xFF7B3FE4, // purple
        0xFF3498DB, // blue
        0xFFE74C3C, // red
        0xFF2ECC71, // green
        0xFFF39C12, // orange
        0xFF1ABC9C, // teal
    };

    // 내부 아이템: 전체 + 팀원 목록
    private static class Item {
        long id;
        String name;
        Item(long id, String name) { this.id = id; this.name = name; }
    }

    private final List<Item> items = new ArrayList<>();
    private long selectedId = -1L;
    private final OnMemberFilterListener listener;

    public MemberFilterAdapter(OnMemberFilterListener listener) {
        this.listener = listener;
        items.add(new Item(-1L, "전체"));
    }

    public void setMembers(List<MemberResponse> memberList) {
        items.clear();
        items.add(new Item(-1L, "전체"));
        for (MemberResponse m : memberList) {
            items.add(new Item(m.getUserId(), m.getName()));
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member_chip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = items.get(position);
        boolean isSelected = (item.id == selectedId);

        String name = item.name;
        String initial = name != null && !name.isEmpty() ? String.valueOf(name.charAt(0)) : "?";
        holder.tvAvatar.setText(initial);

        // 아바타 색상: 선택된 경우 밝게, 아닌 경우 어둡게
        int colorIndex = item.id == -1 ? 0 : (int)(item.id % (AVATAR_COLORS.length - 1)) + 1;
        int baseColor = AVATAR_COLORS[colorIndex];

        if (isSelected) {
            holder.avatarBg.setColor(baseColor);
            holder.avatarBg.setStroke(2, 0xFFD4FF00); // lime border
        } else {
            // dim color
            int r = (baseColor >> 16) & 0xFF;
            int g = (baseColor >> 8) & 0xFF;
            int b = baseColor & 0xFF;
            int dimColor = 0xFF000000 | ((r / 3) << 16) | ((g / 3) << 8) | (b / 3);
            holder.avatarBg.setColor(dimColor);
            holder.avatarBg.setStroke(0, 0); // 선택 해제 시 border 제거
        }

        holder.tvName.setText(name);
        holder.tvName.setTextColor(isSelected ? 0xFFD4FF00 : 0xFF8888AA);

        holder.itemView.setOnClickListener(v -> {
            if (selectedId == item.id) return; // 이미 선택된 경우 무시
            long prevId = selectedId;
            selectedId = item.id;
            // 이전 선택 아이템만 갱신
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).id == prevId) { notifyItemChanged(i); break; }
            }
            // 새 선택 아이템 갱신
            notifyItemChanged(holder.getAdapterPosition());
            listener.onFilter(item.id);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName;
        final GradientDrawable avatarBg;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_member_avatar);
            tvName = itemView.findViewById(R.id.tv_member_name);
            avatarBg = new GradientDrawable();
            avatarBg.setShape(GradientDrawable.OVAL);
            tvAvatar.setBackground(avatarBg);
        }
    }
}
