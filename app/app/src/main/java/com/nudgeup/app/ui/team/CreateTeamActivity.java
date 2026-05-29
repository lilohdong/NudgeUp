package com.nudgeup.app.ui.team;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.nudgeup.app.R;

public class CreateTeamActivity extends AppCompatActivity {

    private static final int[] TEAM_COLORS = {
        0xFF7B3FE4, // purple
        0xFF3498DB, // blue
        0xFFE74C3C, // red
        0xFF2ECC71, // green
        0xFFF39C12, // orange
        0xFF1ABC9C, // teal
    };

    private int selectedColor = TEAM_COLORS[0];
    private View lastSelectedView = null;
    private CreateTeamViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_team);

        viewModel = new ViewModelProvider(this, new CreateTeamViewModel.Factory())
                .get(CreateTeamViewModel.class);

        EditText etTeamName = findViewById(R.id.et_team_name);
        LinearLayout colorPickerContainer = findViewById(R.id.color_picker_container);
        Button btnCreate = findViewById(R.id.btn_create);
        View btnClose = findViewById(R.id.btn_close);

        btnClose.setOnClickListener(v -> finish());

        // 색상 선택기 생성
        int sizeDp = 40;
        float density = getResources().getDisplayMetrics().density;
        int sizePx = (int)(sizeDp * density);
        int marginPx = (int)(10 * density);

        for (int i = 0; i < TEAM_COLORS.length; i++) {
            final int color = TEAM_COLORS[i];

            View colorCircle = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizePx, sizePx);
            params.setMarginEnd(marginPx);
            colorCircle.setLayoutParams(params);

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(color);
            colorCircle.setBackground(bg);
            // 색상 값을 tag로 저장해 복원 시 사용
            colorCircle.setTag(color);

            // 첫 번째 색상 기본 선택
            if (i == 0) {
                bg.setStroke((int)(3 * density), 0xFFD4FF00);
                lastSelectedView = colorCircle;
            }

            colorCircle.setOnClickListener(v -> {
                selectedColor = color;
                // 이전 선택 테두리 제거 — tag에서 원래 색상 복원
                if (lastSelectedView != null) {
                    int prevColor = (int) lastSelectedView.getTag();
                    GradientDrawable prevBg = new GradientDrawable();
                    prevBg.setShape(GradientDrawable.OVAL);
                    prevBg.setColor(prevColor);
                    lastSelectedView.setBackground(prevBg);
                    lastSelectedView.setTag(prevColor); // tag 유지
                }
                // 새 선택 테두리 적용
                GradientDrawable newBg = new GradientDrawable();
                newBg.setShape(GradientDrawable.OVAL);
                newBg.setColor(color);
                newBg.setStroke((int)(3 * density), 0xFFD4FF00);
                v.setBackground(newBg);
                lastSelectedView = v;
            });

            colorPickerContainer.addView(colorCircle);
        }

        viewModel.getCreateResult().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "팀 생성에 실패했습니다.", Toast.LENGTH_SHORT).show();
                btnCreate.setEnabled(true);
            }
        });

        viewModel.getNetworkError().observe(this, error -> {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            btnCreate.setEnabled(true);
        });

        btnCreate.setOnClickListener(v -> {
            String name = etTeamName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "팀 이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            btnCreate.setEnabled(false);
            viewModel.createTeam(name);
        });
    }
}
