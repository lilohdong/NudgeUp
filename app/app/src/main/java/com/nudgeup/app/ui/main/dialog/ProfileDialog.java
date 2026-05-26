package com.nudgeup.app.ui.main.dialog;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.nudgeup.app.NudgeUpApplication;
import com.nudgeup.app.R;
import com.nudgeup.app.ui.auth.LoginActivity;

public class ProfileDialog extends DialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_profile, container, false);

        // 아바타: 라임 원 + 어두운 글자
        TextView tvAvatar = view.findViewById(R.id.tv_profile_avatar);
        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        avatarBg.setColor(0xFFD4FF00); // lime
        tvAvatar.setBackground(avatarBg);
        tvAvatar.setTextColor(0xFF13131F); // dark text on lime

        // 목업 사용자 정보 설정
        // TODO: 백엔드 연결 후 TokenManager / API로 실제 데이터 사용
        String mockName = "이원호";
        String mockRole = "팀장";
        String mockTeamName = "개발팀";
        String mockInviteCode = "DEV001";

        ((TextView) view.findViewById(R.id.tv_profile_avatar)).setText(
                mockName.length() > 0 ? String.valueOf(mockName.charAt(0)) : "?");
        ((TextView) view.findViewById(R.id.tv_user_name)).setText(mockName);
        ((TextView) view.findViewById(R.id.tv_user_role)).setText(mockRole);
        ((TextView) view.findViewById(R.id.tv_current_team)).setText(mockTeamName);
        ((TextView) view.findViewById(R.id.tv_invite_code)).setText(mockInviteCode);

        // 로그아웃 버튼
        Button btnLogout = view.findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> {
            NudgeUpApplication.getTokenManager().clearTokens();
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            dismiss();
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // 다이얼로그 스타일: 투명 배경 + 화면 너비 88%
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            int widthPx = (int) (requireContext().getResources().getDisplayMetrics().widthPixels * 0.88f);
            dialog.getWindow().setLayout(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
