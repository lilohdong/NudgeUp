package com.nudgeup.app.ui.nudge;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.nudgeup.app.R;
import java.util.List;

public class NudgeInboxActivity extends AppCompatActivity {

    private NudgeLogAdapter adapter;
    private NudgeInboxViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nudge_inbox);

        viewModel = new ViewModelProvider(this).get(NudgeInboxViewModel.class);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rv_nudge_inbox);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NudgeLogAdapter();
        rv.setAdapter(adapter);

        viewModel.getNudgeLogs().observe(this, data -> {
            boolean isEmpty = (data == null || data.isEmpty());
            adapter.submitList(data);
            rv.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            findViewById(R.id.tv_empty).setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        });

        viewModel.getLoadFailed().observe(this, failed -> {
            if (Boolean.TRUE.equals(failed)) {
                rv.setVisibility(View.GONE);
                findViewById(R.id.tv_empty).setVisibility(View.VISIBLE);
            }
        });

        viewModel.loadNudgeLogs();
    }
}
