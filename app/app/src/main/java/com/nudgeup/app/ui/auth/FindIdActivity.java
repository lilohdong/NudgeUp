package com.nudgeup.app.ui.auth;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.nudgeup.app.R;

public class FindIdActivity extends AppCompatActivity {

    private EditText etEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_id);

        etEmail = findViewById(R.id.et_email);
        Button btnBack = findViewById(R.id.btn_back);
        Button btnFindId = findViewById(R.id.btn_find_id);
        TextView tvBackToLogin = findViewById(R.id.tv_back_to_login);

        btnBack.setOnClickListener(v -> finish());
        tvBackToLogin.setOnClickListener(v -> finish());

        btnFindId.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            // TODO: 백엔드 연결 후 실제 API 호출
            Toast.makeText(this, "입력하신 이메일로 아이디를 전송했습니다.", Toast.LENGTH_LONG).show();
        });
    }
}
