package com.nudgeup.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.nudgeup.app.NudgeUpApplication;
import com.nudgeup.app.R;
import com.nudgeup.app.ui.main.MainActivity;

public class RegisterActivity extends AppCompatActivity {

    private RegisterViewModel viewModel;
    private EditText etId, etName, etEmail, etPassword, etPasswordConfirm;
    private ImageButton btnTogglePassword, btnTogglePasswordConfirm;
    private boolean isPwVisible = false, isPwConfirmVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        viewModel = new ViewModelProvider(this, new RegisterViewModel.Factory(NudgeUpApplication.getTokenManager()))
                .get(RegisterViewModel.class);

        etId = findViewById(R.id.et_id);
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etPasswordConfirm = findViewById(R.id.et_password_confirm);
        btnTogglePassword = findViewById(R.id.btn_toggle_password);
        btnTogglePasswordConfirm = findViewById(R.id.btn_toggle_password_confirm);
        Button btnBack = findViewById(R.id.btn_back);
        Button btnRegister = findViewById(R.id.btn_register);
        CheckBox cbTerms = findViewById(R.id.cb_terms);
        TextView tvLogin = findViewById(R.id.tv_login);

        btnBack.setOnClickListener(v -> finish());

        btnTogglePassword.setOnClickListener(v -> {
            isPwVisible = !isPwVisible;
            etPassword.setInputType(isPwVisible
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePassword.setImageResource(isPwVisible ? R.drawable.ic_visibility_off : R.drawable.ic_visibility);
            etPassword.setSelection(etPassword.getText().length());
        });

        btnTogglePasswordConfirm.setOnClickListener(v -> {
            isPwConfirmVisible = !isPwConfirmVisible;
            etPasswordConfirm.setInputType(isPwConfirmVisible
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePasswordConfirm.setImageResource(isPwConfirmVisible ? R.drawable.ic_visibility_off : R.drawable.ic_visibility);
            etPasswordConfirm.setSelection(etPasswordConfirm.getText().length());
        });

        tvLogin.setOnClickListener(v -> finish());

        btnRegister.setOnClickListener(v -> {
            String userId = etId.getText().toString().trim();
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String passwordConfirm = etPasswordConfirm.getText().toString().trim();

            if (userId.isEmpty() || name.isEmpty() || email.isEmpty()
                    || password.isEmpty() || passwordConfirm.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(passwordConfirm)) {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!cbTerms.isChecked()) {
                Toast.makeText(this, "이용약관에 동의해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.register(userId, name, email, password);
        });

        viewModel.getRegisterResult().observe(this, result -> {
            if (result.isSuccess()) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
