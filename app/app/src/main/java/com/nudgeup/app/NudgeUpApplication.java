package com.nudgeup.app;

import android.app.Application;
import com.nudgeup.app.data.local.TokenManager;

public class NudgeUpApplication extends Application {

    private static NudgeUpApplication instance;
    private TokenManager tokenManager;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        tokenManager = TokenManager.getInstance(this);
    }

    public static NudgeUpApplication getInstance() {
        return instance;
    }

    public static TokenManager getTokenManager() {
        return instance.tokenManager;
    }
}
