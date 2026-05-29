package com.nudgeup.app.ui.task;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.nudgeup.app.R;
import com.nudgeup.app.data.dto.response.MemberResponse;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CreateTaskActivity extends AppCompatActivity {

    private CreateTaskViewModel viewModel;
    private EditText etTitle, etDescription;
    private TextView tvCharCount, tvAssignee, tvDueDate;

    private long selectedAssigneeId = -1;
    private String selectedDueDate = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        long teamId = getIntent().getLongExtra("teamId", -1);
        viewModel = new ViewModelProvider(this, new CreateTaskViewModel.Factory()).get(CreateTaskViewModel.class);
        viewModel.loadTeamMembers(teamId);

        etTitle = findViewById(R.id.et_title);
        etDescription = findViewById(R.id.et_description);
        tvCharCount = findViewById(R.id.tv_char_count);
        tvAssignee = findViewById(R.id.tv_assignee);
        tvDueDate = findViewById(R.id.tv_due_date);
        LinearLayout btnSelectAssignee = findViewById(R.id.btn_select_assignee);
        LinearLayout btnSelectDueDate = findViewById(R.id.btn_select_due_date);
        android.widget.Button btnCreate = findViewById(R.id.btn_create);
        android.widget.Button btnCancel = findViewById(R.id.btn_cancel);
        android.widget.ImageButton btnClose = findViewById(R.id.btn_close);

        // 글자 수 카운터
        etDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                tvCharCount.setText(s.length() + "/1000");
            }
        });

        // 담당자 선택 (AlertDialog)
        btnSelectAssignee.setOnClickListener(v -> {
            List<MemberResponse> memberList = viewModel.getMembers().getValue();
            if (memberList == null || memberList.isEmpty()) {
                Toast.makeText(this, "팀원 정보를 불러오는 중입니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            String[] names = new String[memberList.size() + 1];
            names[0] = "미지정";
            for (int i = 0; i < memberList.size(); i++) {
                names[i + 1] = memberList.get(i).getName();
            }
            new AlertDialog.Builder(this)
                    .setTitle("담당자 선택")
                    .setItems(names, (dialog, which) -> {
                        if (which == 0) {
                            selectedAssigneeId = -1;
                            tvAssignee.setText("담당자 지정");
                            tvAssignee.setTextColor(getResources().getColor(R.color.text_hint_color, null));
                        } else {
                            MemberResponse m = memberList.get(which - 1);
                            selectedAssigneeId = m.getUserId();
                            tvAssignee.setText(m.getName());
                            tvAssignee.setTextColor(getResources().getColor(R.color.text_primary, null));
                        }
                    })
                    .show();
        });

        // 마감일 선택
        btnSelectDueDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                selectedDueDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);
                tvDueDate.setText(selectedDueDate);
                tvDueDate.setTextColor(getResources().getColor(R.color.text_primary, null));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // 작업 생성
        btnCreate.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(this, "작업 제목을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            String description = etDescription.getText().toString().trim();
            Long assigneeId = selectedAssigneeId == -1 ? null : selectedAssigneeId;
            viewModel.createTask(teamId, title, description.isEmpty() ? null : description, assigneeId, selectedDueDate);
        });

        btnCancel.setOnClickListener(v -> finish());

        // 닫기
        btnClose.setOnClickListener(v -> finish());

        viewModel.getCreateResult().observe(this, success -> {
            if (success != null) {
                if (success) finish();
                else Toast.makeText(this, "작업 생성에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getNetworkError().observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
    }
}
