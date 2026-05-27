package com.nudgeup.app.ui.auth;

import android.content.Context;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.nudgeup.app.R;
import com.nudgeup.app.data.local.TokenManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class LoginActivitySplashTest {

    @Before
    public void clearTokensBeforeEach() {
        Context ctx = ApplicationProvider.getApplicationContext();
        TokenManager.getInstance(ctx).clearTokens();
    }

    @Test
    public void whenNotLoggedIn_loginButtonIsVisible() {
        try (ActivityScenario<LoginActivity> scenario =
                     ActivityScenario.launch(LoginActivity.class)) {
            Espresso.onView(withId(R.id.btn_login))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void whenNotLoggedIn_emailInputIsVisible() {
        try (ActivityScenario<LoginActivity> scenario =
                     ActivityScenario.launch(LoginActivity.class)) {
            Espresso.onView(withId(R.id.et_email))
                    .check(matches(isDisplayed()));
        }
    }
}
