package com.nudgeup.app.ui.main.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.nudgeup.app.NudgeUpApplication;
import com.nudgeup.app.R;
import com.nudgeup.app.util.ToastUtils;
import com.nudgeup.app.data.repository.UserRepository;
import com.nudgeup.app.ui.auth.LoginActivity;

public class ProfileDialog extends DialogFragment {

    private static final String ARG_IS_LEADER = "is_leader";

    private static final String PREFS_NAME  = "profile_prefs";
    private static final String KEY_NAME    = "profile_name";
    private static final String KEY_COLOR   = "profile_avatar_color";
    private static final int    DEFAULT_COLOR = 0xFFD4FF00; // 시그니처 라임

    public static ProfileDialog newInstance(boolean isLeader) {
        ProfileDialog dialog = new ProfileDialog();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_LEADER, isLeader);
        dialog.setArguments(args);
        return dialog;
    }

    /** 선택 가능한 아바타 색상 팔레트 (8가지) */
    private static final int[] SWATCH_COLORS = {
        0xFFD4FF00, // lime  — 시그니처
        0xFF7B3FE4, // purple
        0xFF3498DB, // blue
        0xFFE74C3C, // red
        0xFF2ECC71, // green
        0xFFF39C12, // orange
        0xFF1ABC9C, // teal
        0xFFE91E8C, // pink
    };

    // ─── 상태 ───
    private boolean isEditMode = false;
    private int     selectedColor;
    private String  currentName;

    // ─── 의존성 ───
    private final UserRepository userRepo = new UserRepository();
    private Button btnSaveProfile;

    // ─── View 참조 ───
    private TextView    tvAvatar;
    private TextView    tvUserName;
    private EditText    etDisplayName;
    private LinearLayout colorSwatchesContainer;
    private LinearLayout layoutViewMode;
    private LinearLayout layoutEditMode;
    private FrameLayout  flAvatarEditBadge;
    private ImageButton  btnEditProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_profile, container, false);

        // ── SharedPreferences 로드 ──
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedName = prefs.getString(KEY_NAME, null);
        if (savedName != null) {
            currentName = savedName;
        } else {
            String tokenUsername = NudgeUpApplication.getTokenManager().getUsername();
            currentName = tokenUsername != null ? tokenUsername : "";
        }
        selectedColor = prefs.getInt(KEY_COLOR, DEFAULT_COLOR);

        // ── View 바인딩 ──
        tvAvatar              = view.findViewById(R.id.tv_profile_avatar);
        tvUserName            = view.findViewById(R.id.tv_user_name);
        etDisplayName         = view.findViewById(R.id.et_display_name);
        colorSwatchesContainer = view.findViewById(R.id.color_swatches_container);
        layoutViewMode        = view.findViewById(R.id.layout_view_mode);
        layoutEditMode        = view.findViewById(R.id.layout_edit_mode);
        flAvatarEditBadge     = view.findViewById(R.id.fl_avatar_edit_badge);
        btnEditProfile        = view.findViewById(R.id.btn_edit_profile);

        boolean isLeader = getArguments() != null && getArguments().getBoolean(ARG_IS_LEADER, false);
        ((TextView) view.findViewById(R.id.tv_user_role)).setText(isLeader ? "팀장" : "팀원");

        // ── 초기 아바타·이름 렌더링 ──
        applyAvatarColor(selectedColor);
        refreshNameDisplay();

        // ── 편집 버튼 (연필 ↔ X 토글) ──
        btnEditProfile.setOnClickListener(v -> toggleEditMode(prefs));

        btnSaveProfile = view.findViewById(R.id.btn_save_profile);

        // ── 취소 버튼 ──
        view.findViewById(R.id.btn_cancel_edit).setOnClickListener(v -> {
            // 변경 사항 원상 복구
            selectedColor = prefs.getInt(KEY_COLOR, DEFAULT_COLOR);
            applyAvatarColor(selectedColor);
            exitEditMode();
        });

        // ── 저장 버튼 ──
        btnSaveProfile.setOnClickListener(v -> {
            String newName = etDisplayName.getText().toString().trim();
            if (newName.isEmpty()) {
                etDisplayName.setError("이름을 입력하세요");
                return;
            }

            boolean nameChanged = !newName.equals(currentName);
            int savedColor = prefs.getInt(KEY_COLOR, DEFAULT_COLOR);
            boolean colorChanged = (selectedColor != savedColor);

            if (!nameChanged && !colorChanged) {
                exitEditMode();
                return;
            }

            btnSaveProfile.setEnabled(false);

            // 색상은 서버 제한 없으므로 즉시 로컬 저장
            if (colorChanged) {
                prefs.edit().putInt(KEY_COLOR, selectedColor).apply();
            }

            // UI 낙관적 업데이트 (이름 표시만, prefs 저장은 API 성공 후)
            String prevName = currentName;
            currentName = newName;
            refreshNameDisplay();
            exitEditMode();

            final int[] pending = {(nameChanged ? 1 : 0) + (colorChanged ? 1 : 0)};
            final boolean[] anyFailed = {false};

            Runnable checkDone = () -> {
                pending[0]--;
                if (pending[0] == 0) {
                    if (isAdded()) btnSaveProfile.setEnabled(true);
                    if (!anyFailed[0] && isAdded()) {
                        ToastUtils.show(requireContext(), "프로필이 저장됐습니다 ✓");
                    }
                }
            };

            if (nameChanged) {
                final String nameToSave = newName;
                userRepo.updateUsername(nameToSave).enqueue(new retrofit2.Callback<com.nudgeup.app.data.dto.response.ApiResponse<com.nudgeup.app.data.dto.response.UserResponse>>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.nudgeup.app.data.dto.response.ApiResponse<com.nudgeup.app.data.dto.response.UserResponse>> call,
                                           retrofit2.Response<com.nudgeup.app.data.dto.response.ApiResponse<com.nudgeup.app.data.dto.response.UserResponse>> response) {
                        if (response.isSuccessful()) {
                            prefs.edit().putString(KEY_NAME, nameToSave).apply();
                        } else {
                            anyFailed[0] = true;
                            currentName = prevName;
                            if (isAdded()) {
                                refreshNameDisplay();
                                ToastUtils.show(requireContext(), "이름 변경 실패: 7일 이내 재변경 불가");
                            }
                        }
                        checkDone.run();
                    }
                    @Override
                    public void onFailure(retrofit2.Call<com.nudgeup.app.data.dto.response.ApiResponse<com.nudgeup.app.data.dto.response.UserResponse>> call, Throwable t) {
                        anyFailed[0] = true;
                        currentName = prevName;
                        if (isAdded()) {
                            refreshNameDisplay();
                            ToastUtils.show(requireContext(), "네트워크 오류");
                        }
                        checkDone.run();
                    }
                });
            }

            if (colorChanged) {
                String colorHex = String.format("#%06X", 0xFFFFFF & selectedColor);
                userRepo.updateColor(colorHex).enqueue(new retrofit2.Callback<com.nudgeup.app.data.dto.response.ApiResponse<com.nudgeup.app.data.dto.response.UserResponse>>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.nudgeup.app.data.dto.response.ApiResponse<com.nudgeup.app.data.dto.response.UserResponse>> call,
                                           retrofit2.Response<com.nudgeup.app.data.dto.response.ApiResponse<com.nudgeup.app.data.dto.response.UserResponse>> response) {
                        if (!response.isSuccessful()) {
                            anyFailed[0] = true;
                            if (isAdded()) ToastUtils.show(requireContext(), "색상 변경 실패");
                        }
                        checkDone.run();
                    }
                    @Override
                    public void onFailure(retrofit2.Call<com.nudgeup.app.data.dto.response.ApiResponse<com.nudgeup.app.data.dto.response.UserResponse>> call, Throwable t) {
                        anyFailed[0] = true;
                        if (isAdded()) ToastUtils.show(requireContext(), "네트워크 오류");
                        checkDone.run();
                    }
                });
            }
        });

        // ── 로그아웃 ──
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

    // ─────────────────────────────────────────
    //  편집 모드 토글
    // ─────────────────────────────────────────

    private void toggleEditMode(SharedPreferences prefs) {
        if (isEditMode) {
            // 취소와 동일: 원상 복구 후 뷰 모드
            selectedColor = prefs.getInt(KEY_COLOR, DEFAULT_COLOR);
            applyAvatarColor(selectedColor);
            exitEditMode();
        } else {
            enterEditMode();
        }
    }

    private void enterEditMode() {
        isEditMode = true;

        etDisplayName.setText(currentName);
        etDisplayName.setSelection(currentName.length());

        tvUserName.setVisibility(View.GONE);
        etDisplayName.setVisibility(View.VISIBLE);
        layoutViewMode.setVisibility(View.GONE);
        layoutEditMode.setVisibility(View.VISIBLE);
        flAvatarEditBadge.setVisibility(View.VISIBLE);
        btnEditProfile.setImageResource(R.drawable.ic_close);

        // 색상 스와치 생성
        buildColorSwatches();

        // 키보드 포커스
        etDisplayName.requestFocus();
        InputMethodManager imm = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(etDisplayName, InputMethodManager.SHOW_IMPLICIT);
    }

    private void exitEditMode() {
        isEditMode = false;

        tvUserName.setVisibility(View.VISIBLE);
        etDisplayName.setVisibility(View.GONE);
        layoutViewMode.setVisibility(View.VISIBLE);
        layoutEditMode.setVisibility(View.GONE);
        flAvatarEditBadge.setVisibility(View.GONE);
        btnEditProfile.setImageResource(R.drawable.ic_edit);

        // 키보드 닫기
        InputMethodManager imm = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getView() != null) {
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
    }

    // ─────────────────────────────────────────
    //  아바타 렌더링
    // ─────────────────────────────────────────

    /** 아바타 원 색상 적용 + 텍스트 대비색 자동 결정 */
    private void applyAvatarColor(int color) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(color);
        tvAvatar.setBackground(bg);
        // 밝기에 따라 어두운/밝은 이니셜 텍스트 색상 선택
        tvAvatar.setTextColor(perceivedLuminance(color) > 128 ? 0xFF13131F : 0xFFFFFFFF);
    }

    private void refreshNameDisplay() {
        String initial = (currentName != null && !currentName.isEmpty())
                ? String.valueOf(currentName.charAt(0)) : "?";
        tvAvatar.setText(initial);
        tvUserName.setText(currentName);
    }

    // ─────────────────────────────────────────
    //  색상 스와치 (편집 모드용)
    // ─────────────────────────────────────────

    private void buildColorSwatches() {
        colorSwatchesContainer.removeAllViews();
        int size   = dpToPx(36);
        int margin = dpToPx(10);

        for (int color : SWATCH_COLORS) {
            // 외부 FrameLayout: 선택 시 흰 테두리 링 역할
            FrameLayout wrapper = new FrameLayout(requireContext());
            LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(
                    size + dpToPx(6), size + dpToPx(6));
            wlp.setMarginEnd(margin);
            wrapper.setLayoutParams(wlp);

            boolean isSelected = (color == selectedColor);
            if (isSelected) {
                GradientDrawable ring = new GradientDrawable();
                ring.setShape(GradientDrawable.OVAL);
                ring.setColor(0x00000000);
                ring.setStroke(dpToPx(2), 0xFFFFFFFF);
                wrapper.setBackground(ring);
            }

            // 내부 색상 원
            View circle = new View(requireContext());
            FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(size, size);
            clp.gravity = android.view.Gravity.CENTER;
            circle.setLayoutParams(clp);
            GradientDrawable circleBg = new GradientDrawable();
            circleBg.setShape(GradientDrawable.OVAL);
            circleBg.setColor(color);
            circle.setBackground(circleBg);

            wrapper.addView(circle);

            // 클릭: 실시간 미리보기 + 스와치 선택 표시 갱신
            final int clickedColor = color;
            wrapper.setOnClickListener(v -> {
                selectedColor = clickedColor;
                applyAvatarColor(clickedColor);
                buildColorSwatches(); // 선택 링 재생성
            });

            colorSwatchesContainer.addView(wrapper);
        }
    }

    // ─────────────────────────────────────────
    //  유틸
    // ─────────────────────────────────────────

    /** 색상의 체감 밝기 (0~255) */
    private int perceivedLuminance(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8)  & 0xFF;
        int b =  color        & 0xFF;
        return (int)(0.299 * r + 0.587 * g + 0.114 * b);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    // ─────────────────────────────────────────
    //  다이얼로그 스타일 (투명 배경 + 88% 너비)
    // ─────────────────────────────────────────

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            int widthPx = (int)(requireContext().getResources()
                    .getDisplayMetrics().widthPixels * 0.88f);
            dialog.getWindow().setLayout(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
