package com.nudgeup.app.ui.auth;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.nudgeup.app.R;
import com.nudgeup.app.util.ToastUtils;

import java.util.Locale;

public class FindPasswordActivity extends AppCompatActivity {

    private int currentStep = 1;

    // Step 1
    private LinearLayout layoutStep1;
    private EditText etStep1Email;

    // Step 2
    private LinearLayout layoutStep2;
    private EditText etStep2EmailReadonly, etCode;
    private TextView tvTimer;
    private CountDownTimer countDownTimer;

    // Step 3
    private LinearLayout layoutStep3;
    private EditText etNewPassword, etNewPasswordConfirm;

    // Step 인디케이터
    private TextView tvStep1Circle, tvStep2Circle, tvStep3Circle;
    private TextView tvLabelStep1, tvLabelStep2, tvLabelStep3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_password);

        // 뷰 바인딩
        layoutStep1 = findViewById(R.id.layout_step1);
        layoutStep2 = findViewById(R.id.layout_step2);
        layoutStep3 = findViewById(R.id.layout_step3);
        etStep1Email = findViewById(R.id.et_step1_email);
        etStep2EmailReadonly = findViewById(R.id.et_step2_email_readonly);
        etCode = findViewById(R.id.et_code);
        tvTimer = findViewById(R.id.tv_timer);
        etNewPassword = findViewById(R.id.et_new_password);
        etNewPasswordConfirm = findViewById(R.id.et_new_password_confirm);
        tvStep1Circle = findViewById(R.id.tv_step1_circle);
        tvStep2Circle = findViewById(R.id.tv_step2_circle);
        tvStep3Circle = findViewById(R.id.tv_step3_circle);
        tvLabelStep1 = findViewById(R.id.tv_label_step1);
        tvLabelStep2 = findViewById(R.id.tv_label_step2);
        tvLabelStep3 = findViewById(R.id.tv_label_step3);

        Button btnBack = findViewById(R.id.btn_back);
        Button btnStep1Next = findViewById(R.id.btn_step1_next);
        Button btnStep2Confirm = findViewById(R.id.btn_step2_confirm);
        Button btnStep3Change = findViewById(R.id.btn_step3_change);
        TextView tvResend = findViewById(R.id.tv_resend);
        TextView tvBackToLogin = findViewById(R.id.tv_back_to_login);

        ImageButton btnToggleNewPw = findViewById(R.id.btn_toggle_new_pw);
        ImageButton btnToggleNewPwConfirm = findViewById(R.id.btn_toggle_new_pw_confirm);

        btnBack.setOnClickListener(v -> finish());
        tvBackToLogin.setOnClickListener(v -> finish());

        // Step 1: 이메일 입력 → Step 2로
        btnStep1Next.setOnClickListener(v -> {
            String email = etStep1Email.getText().toString().trim();
            if (email.isEmpty()) {
                ToastUtils.show(this, "이메일을 입력해주세요.");
                return;
            }
            etStep2EmailReadonly.setText(email);
            // TODO: 백엔드 연결 후 인증번호 발송 API 호출
            goToStep(2);
            startTimer();
        });

        // Step 2: 인증번호 확인 → Step 3으로
        btnStep2Confirm.setOnClickListener(v -> {
            String code = etCode.getText().toString().trim();
            if (code.isEmpty()) {
                ToastUtils.show(this, "인증번호를 입력해주세요.");
                return;
            }
            // TODO: 백엔드 연결 후 인증번호 검증 API 호출
            if (countDownTimer != null) countDownTimer.cancel();
            goToStep(3);
        });

        // 이메일 재전송
        tvResend.setOnClickListener(v -> {
            if (countDownTimer != null) countDownTimer.cancel();
            startTimer();
            ToastUtils.show(this, "인증번호를 재전송했습니다.");
        });

        // Step 3: 비밀번호 변경
        btnStep3Change.setOnClickListener(v -> {
            String newPw = etNewPassword.getText().toString().trim();
            String newPwConfirm = etNewPasswordConfirm.getText().toString().trim();
            if (newPw.isEmpty()) {
                ToastUtils.show(this, "새 비밀번호를 입력해주세요.");
                return;
            }
            if (!newPw.equals(newPwConfirm)) {
                ToastUtils.show(this, "비밀번호가 일치하지 않습니다.");
                return;
            }
            // TODO: 백엔드 연결 후 비밀번호 변경 API 호출
            ToastUtils.show(this, "비밀번호가 변경되었습니다.");
            finish();
        });

        // 비밀번호 토글
        final boolean[] isPwVisible = {false};
        final boolean[] isPwConfirmVisible = {false};

        btnToggleNewPw.setOnClickListener(v -> {
            isPwVisible[0] = !isPwVisible[0];
            etNewPassword.setInputType(isPwVisible[0]
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnToggleNewPw.setImageResource(isPwVisible[0] ? R.drawable.ic_visibility_off : R.drawable.ic_visibility);
            etNewPassword.setSelection(etNewPassword.getText().length());
        });

        btnToggleNewPwConfirm.setOnClickListener(v -> {
            isPwConfirmVisible[0] = !isPwConfirmVisible[0];
            etNewPasswordConfirm.setInputType(isPwConfirmVisible[0]
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnToggleNewPwConfirm.setImageResource(isPwConfirmVisible[0] ? R.drawable.ic_visibility_off : R.drawable.ic_visibility);
            etNewPasswordConfirm.setSelection(etNewPasswordConfirm.getText().length());
        });
    }

    private void goToStep(int step) {
        currentStep = step;
        layoutStep1.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        layoutStep2.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        layoutStep3.setVisibility(step == 3 ? View.VISIBLE : View.GONE);
        updateStepIndicator(step);
    }

    private void updateStepIndicator(int step) {
        // 원 배경 업데이트
        tvStep1Circle.setBackground(getDrawable(step >= 1 ? R.drawable.bg_step_active : R.drawable.bg_step_inactive));
        tvStep2Circle.setBackground(getDrawable(step >= 2 ? R.drawable.bg_step_active : R.drawable.bg_step_inactive));
        tvStep3Circle.setBackground(getDrawable(step >= 3 ? R.drawable.bg_step_active : R.drawable.bg_step_inactive));

        // 라벨 색상 업데이트
        int activeColor = getResources().getColor(R.color.color_primary, getTheme());
        int inactiveColor = getResources().getColor(R.color.text_secondary, getTheme());
        tvLabelStep1.setTextColor(step >= 1 ? activeColor : inactiveColor);
        tvLabelStep2.setTextColor(step >= 2 ? activeColor : inactiveColor);
        tvLabelStep3.setTextColor(step >= 3 ? activeColor : inactiveColor);
    }

    private void startTimer() {
        // 3분 타이머
        countDownTimer = new CountDownTimer(180000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = millisUntilFinished / 60000;
                long seconds = (millisUntilFinished % 60000) / 1000;
                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                tvTimer.setText("00:00");
                ToastUtils.show(FindPasswordActivity.this, "인증 시간이 만료되었습니다.");
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
