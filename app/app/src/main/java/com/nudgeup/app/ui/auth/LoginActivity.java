package com.nudgeup.app.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.nudgeup.app.NudgeUpApplication;
import com.nudgeup.app.R;
import com.nudgeup.app.util.ToastUtils;
import com.nudgeup.app.ui.main.MainActivity;

public class LoginActivity extends AppCompatActivity {

    private LoginViewModel viewModel;
    private EditText etUserId, etPassword;
    private ImageButton btnTogglePassword;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (NudgeUpApplication.getTokenManager().isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        viewModel = new ViewModelProvider(this, new LoginViewModel.Factory(NudgeUpApplication.getTokenManager()))
                .get(LoginViewModel.class);

        etUserId = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnTogglePassword = findViewById(R.id.btn_toggle_password);
        Button btnLogin = findViewById(R.id.btn_login);
        Button btnBack = findViewById(R.id.btn_back);
        Button btnRegister = findViewById(R.id.btn_register_outline);
        CheckBox cbSaveId = findViewById(R.id.cb_save_id);
        TextView tvFindPassword = findViewById(R.id.tv_find_password);

        // 저장된 아이디 불러오기
        SharedPreferences prefs = getSharedPreferences("nudgeup_prefs", MODE_PRIVATE);
        boolean isSaved = prefs.getBoolean("save_id", false);
        cbSaveId.setChecked(isSaved);
        if (isSaved) etUserId.setText(prefs.getString("saved_id", ""));

        // 뒤로가기 (앱 종료)
        btnBack.setOnClickListener(v -> finish());

        // 비밀번호 표시 토글
        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        // 아이디 저장 체크박스
        cbSaveId.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean("save_id", isChecked).apply();
        });

        // 비밀번호 찾기
        tvFindPassword.setOnClickListener(v ->
                startActivity(new Intent(this, FindPasswordActivity.class)));

        // 로그인 버튼
        btnLogin.setOnClickListener(v -> {
            String userId = etUserId.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (userId.isEmpty() || password.isEmpty()) {
                ToastUtils.show(this, "아이디와 비밀번호를 입력해주세요.");
                return;
            }
            if (cbSaveId.isChecked()) {
                prefs.edit().putString("saved_id", userId).apply();
            }
            viewModel.login(userId, password);
        });

        // 회원가입 버튼
        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        // 로그인 결과 관찰
        viewModel.getLoginResult().observe(this, result -> {
            if (result.isSuccess()) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                ToastUtils.show(this, result.getMessage());
            }
        });
    }

    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_visibility_off);
        } else {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_visibility);
        }
        etPassword.setSelection(etPassword.getText().length());
    }
}
