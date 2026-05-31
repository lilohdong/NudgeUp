package com.nudgeup.app.ui.nudge;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.nudgeup.app.R;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.NudgeLogItemResponse;
import com.nudgeup.app.data.repository.NudgeRepository;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NudgeInboxActivity extends AppCompatActivity {

    private NudgeLogAdapter adapter;
    private final NudgeRepository nudgeRepository = new NudgeRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nudge_inbox);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rv_nudge_inbox);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NudgeLogAdapter();
        rv.setAdapter(adapter);

        loadNudgeLogs();
    }

    private void loadNudgeLogs() {
        nudgeRepository.getReceivedNudgeLogs().enqueue(
                new Callback<ApiResponse<List<NudgeLogItemResponse>>>() {
                    @Override
                    public void onResponse(
                            Call<ApiResponse<List<NudgeLogItemResponse>>> call,
                            Response<ApiResponse<List<NudgeLogItemResponse>>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {
                            List<NudgeLogItemResponse> data = response.body().getData();
                            boolean isEmpty = (data == null || data.isEmpty());
                            adapter.submitList(data);
                            findViewById(R.id.rv_nudge_inbox)
                                    .setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                            findViewById(R.id.tv_empty)
                                    .setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<NudgeLogItemResponse>>> call,
                                         Throwable t) {
                        findViewById(R.id.rv_nudge_inbox).setVisibility(View.GONE);
                        findViewById(R.id.tv_empty).setVisibility(View.VISIBLE);
                    }
                });
    }
}
