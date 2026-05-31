package com.nudgeup.app.global.network;

import android.content.Intent;
import com.nudgeup.app.NudgeUpApplication;
import com.nudgeup.app.ui.auth.LoginActivity;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        String token = NudgeUpApplication.getTokenManager().getAccessToken();
        Request request = chain.request();
        if (token != null) {
            request = request.newBuilder()
                    .addHeader("Authorization", "Bearer " + token)
                    .build();
        }
        Response response = chain.proceed(request);
        if (response.code() == 401) {
            NudgeUpApplication.getTokenManager().clearTokens();
            Intent intent = new Intent(NudgeUpApplication.getInstance(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            NudgeUpApplication.getInstance().startActivity(intent);
        }
        return response;
    }
}
