package com.nudgeup.app.ui.auth;

import android.content.Context;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.nudgeup.app.R;
import com.nudgeup.app.data.local.TokenManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class LoginActivitySplashTest {

    @Test
    public void whenNotLoggedIn_loginButtonIsVisible() {
        // ActivityScenarioRule은 @Before보다 먼저 Activity를 실행하므로
        // 토큰 초기화 후 수동으로 Activity를 실행한다
        Context ctx = ApplicationProvider.getApplicationContext();
        TokenManager.getInstance(ctx).clearTokens();

        try (ActivityScenario<LoginActivity> scenario =
                     ActivityScenario.launch(LoginActivity.class)) {
            Espresso.onView(withId(R.id.btn_login))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void whenNotLoggedIn_emailInputIsVisible() {
        Context ctx = ApplicationProvider.getApplicationContext();
        TokenManager.getInstance(ctx).clearTokens();

        try (ActivityScenario<LoginActivity> scenario =
                     ActivityScenario.launch(LoginActivity.class)) {
            Espresso.onView(withId(R.id.et_email))
                    .check(matches(isDisplayed()));
        }
    }
}
