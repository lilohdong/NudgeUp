package com.nudgeup.app.ui.nudge;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.nudgeup.app.data.dto.response.ApiResponse;
import com.nudgeup.app.data.dto.response.NudgeLogItemResponse;
import com.nudgeup.app.data.repository.NudgeRepository;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NudgeInboxViewModel extends ViewModel {

    private final NudgeRepository nudgeRepository = new NudgeRepository();

    private final MutableLiveData<List<NudgeLogItemResponse>> nudgeLogs = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadFailed = new MutableLiveData<>();

    public LiveData<List<NudgeLogItemResponse>> getNudgeLogs() { return nudgeLogs; }
    public LiveData<Boolean> getLoadFailed() { return loadFailed; }

    public void loadNudgeLogs() {
        nudgeRepository.getReceivedNudgeLogs().enqueue(
                new Callback<ApiResponse<List<NudgeLogItemResponse>>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<ApiResponse<List<NudgeLogItemResponse>>> call,
                            @NonNull Response<ApiResponse<List<NudgeLogItemResponse>>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {
                            nudgeLogs.setValue(response.body().getData());
                        } else {
                            loadFailed.setValue(true);
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<ApiResponse<List<NudgeLogItemResponse>>> call,
                            @NonNull Throwable t) {
                        loadFailed.setValue(true);
                    }
                });
    }
}
